package com.github.reload.net;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Context.Component;
import com.github.reload.Context.CtxComponent;
import com.github.reload.message.Header;
import com.github.reload.message.NodeID;
import com.github.reload.message.RoutableID;
import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.data.HeadedMessage;
import com.github.reload.net.data.Message;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Send the outgoing messages to neighbor nodes by using the routing table
 */
public class MessageRouter implements Component {

	private final Logger l = Logger.getRootLogger();
	private static final int REQUEST_TIMEOUT = 3000;
	private static final RemovalListener<Long, SettableFuture<Message>> EXP_REQ_LISTERNER = new RemovalListener<Long, SettableFuture<Message>>() {

		@Override
		public void onRemoval(RemovalNotification<Long, SettableFuture<Message>> notification) {
			// Set failure only if request times out
			if (!notification.wasEvicted())
				return;

			SettableFuture<Message> future = notification.getValue();
			future.setException(new RequestTimeoutException());
		}
	};

	@CtxComponent
	private TopologyPlugin topologyPlugin;

	private RoutingTable routingTable;

	private Cache<Long, SettableFuture<Message>> pendingRequests;

	@Override
	public void compStart() {
		this.routingTable = topologyPlugin.getRoutingTable();
		pendingRequests = CacheBuilder.newBuilder().expireAfterWrite(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS).removalListener(EXP_REQ_LISTERNER).build();
	}

	/**
	 * Send the given request message to the destination node into the overlay
	 * and waits for an answer.
	 * Since the action is performed asyncronously, this method returns
	 * immediately and the returned {@link RequestPromise} can be used to
	 * control the delivery of the message and to receive the answer.
	 * 
	 * @param message
	 * @return
	 */
	public ListenableFuture<Message> sendRequestMessage(Message request) {
		ForwardFuture fwdPrm = sendMessage(request);

		final SettableFuture<Message> reqFut = SettableFuture.create();

		// Fail fast if request fails already in the neighbors transmission
		fwdPrm.addListener(new ForwardFutureListener() {

			@Override
			public void operationComplete(ForwardFuture future) throws Exception {
				// Fails only if the forward fails to ALL the neighbor nodes
				if (!future.isSuccess() && future.getSuccessNeighbors().size() == 0) {
					reqFut.setException(future.cause());
				}
			}
		});

		pendingRequests.put(request.getHeader().getTransactionId(), reqFut);

		return reqFut;
	}

	@Subscribe
	public void handleAnswer(Message message) {

		if (!message.getContent().isAnswer())
			return;

		Long transactionId = message.getHeader().getTransactionId();

		SettableFuture<Message> reqFut = pendingRequests.getIfPresent(transactionId);

		if (reqFut == null) {
			l.log(Level.DEBUG, "Unexpected answer dropped: " + message);
			return;
		}

		pendingRequests.invalidate(transactionId);

		reqFut.set(message);
	}

	public ForwardFuture sendMessage(HeadedMessage message) {
		Header header = message.getHeader();

		List<Connection> hops = routingTable.getNextHops(header.getDestinationId());

		ForwardFuture fwdFut = new ForwardFuture(routingTable.keySet());

		for (Connection nextHop : hops) {
			forward(message, nextHop, fwdFut);
		}

		return fwdFut;
	}

	private void forward(HeadedMessage message, Connection conn, ForwardFuture fwdFuture) {
		ChannelFuture chFut = conn.write(message);
		fwdFuture.addChannelFuture(conn.getNodeId(), chFut);
	}

	public boolean getForwadingAction(Header header) {
		RoutableID dest = header.getDestinationId();
		switch (dest.getType()) {
			case NODEID :

				break;

			case RESOURCEID :

				break;

			case OPAQUEID :

				break;
		}

		return false;
	}

	public enum FWD_ACTION {
		DROP, DROP_SILENT, FORWARD, HANDLE;
	}

	/**
	 * This object controls the transmission status of an outgoing
	 * message to the neighbor nodes
	 */
	public static class ForwardFuture extends AbstractFuture<Boolean> {

		private final Set<NodeID> pendingNeighbors;
		private final Map<NodeID, Throwable> failedNeighbors;
		private final Set<NodeID> successNeighbors;

		public ForwardFuture(Set<NodeID> neighbors) {
			this.pendingNeighbors = neighbors;
			successNeighbors = new HashSet<NodeID>();
			failedNeighbors = new HashMap<NodeID, Throwable>();

		}

		public boolean isDone() {
			return pendingNeighbors.size() == 0;
		}

		public boolean isSuccess() {
			return isDone() && failedNeighbors.size() == 0;
		}

		public Map<NodeID, Throwable> getFailedNeighbors() {
			return failedNeighbors;
		}

		public Set<NodeID> getPendingNeighbors() {
			return pendingNeighbors;
		}

		public Set<NodeID> getSuccessNeighbors() {
			return successNeighbors;
		}

		void addChannelFuture(final NodeID neighbor, ChannelFuture chFut) {
			chFut.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					synchronized (future) {
						if (future.isSuccess())
							successNeighbors.add(neighbor);
						else
							failedNeighbors.put(neighbor, future.cause());

						pendingNeighbors.remove(neighbor);

						if (pendingNeighbors.size() == 0) {
							if (isSuccess())
								set(isSuccess());
							else
								setException(new ForwardingException(failedNeighbors));
						}
					}
				}
			});
		}
	}

	public static class ForwardingException extends Exception {

		private final Map<NodeID, Throwable> failures;

		public ForwardingException(Map<NodeID, Throwable> failures) {
			this.failures = failures;
		}

		public Map<NodeID, Throwable> getFailures() {
			return failures;
		}

	}

	public static class RequestTimeoutException extends Exception implements ErrorRespose {

		@Override
		public ErrorType getErrorType() {
			return ErrorType.REQUEST_TIMEOUT;
		}
	}
}
