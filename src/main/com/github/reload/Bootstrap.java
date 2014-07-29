package com.github.reload;

import java.net.InetSocketAddress;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsRepository;
import com.github.reload.conf.Configuration;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.NetworkException;
import com.github.reload.net.connections.AttachConnector;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate.CertificateType;
import com.github.reload.net.ice.ICEHelper;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Connector used for a specific overlay configuration, it configures the
 * local peer to operate with a specific overlay instance
 * 
 */
public abstract class Bootstrap {

	private final Configuration conf;
	private InetSocketAddress localAddr;
	private boolean isOverlayInitiator;
	private boolean isClientMode = false;
	private NodeID localNodeId;

	public Bootstrap(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * @return the data to be send in the join request
	 */
	protected abstract byte[] getJoinData();

	protected abstract CertificateType getCertificateType();

	/**
	 * @return The address where the server will be listening to
	 */
	public InetSocketAddress getLocalAddr() {
		return localAddr;
	}

	/**
	 * Set the address where the server will be listening to.
	 * 
	 * @param attachAddr
	 */
	public void setLocalAddress(InetSocketAddress localAddr) {
		this.localAddr = localAddr;
	}

	/**
	 * Set if this peer is the initiator peer of the overlay (the first peer who
	 * doesn't have to join). If set to true this peer must also set a bootstrap
	 * server otherwise an initialization exception will be thrown on
	 * connecting.
	 * 
	 * @param isOverlayInitiator
	 * @see #setBootstrapAddress(InetSocketAddress)
	 */
	public void setOverlayInitiator(boolean isOverlayInitiator) {
		this.isOverlayInitiator = isOverlayInitiator;
	}

	/**
	 * @return True if this peer is the initiator peer of the overlay (the first
	 *         peer who doesn't have to join)
	 */
	public boolean isOverlayInitiator() {
		return isOverlayInitiator;
	}

	/**
	 * Set if this node must operate as a client rather than as a normal peer.
	 * In client mode the node will not collaborate to the overlay storage and
	 * message routing functionalities. Also the node is not directly reachable
	 * by its node-id, all the messages directed to this node must be forwarded
	 * through the connected neighbor.
	 * 
	 * @param isClientMode
	 */
	public void setClientMode(boolean isClientMode) {
		this.isClientMode = isClientMode;
	}

	/**
	 * Set the node-id of the local peer to be used in this instance. If not
	 * specified, it will be used the first one found in the peer certificate
	 * 
	 * @param localNodeId
	 */
	public void setLocalNodeId(NodeID localNodeId) {
		this.localNodeId = localNodeId;
	}

	/**
	 * Try to connect to the overlay by using the informations provided by this
	 * connector
	 * 
	 * @return The connection handler
	 * @throws InitializationException
	 *             if the initialization of the local peer fails
	 * @throws NetworkException
	 *             if some network error occurs
	 */
	public final ListenableFuture<Overlay> connect() {
		ComponentsRepository.register(MessageBuilder.class);
		ComponentsRepository.register(ConnectionManager.class);
		ComponentsRepository.register(AttachConnector.class);
		ComponentsRepository.register(ICEHelper.class);
		ComponentsRepository.register(MessageRouter.class);
		registerComponents();

		ComponentsContext ctx = ComponentsContext.getDefault();
		ctx.set(Configuration.class, conf);
		ctx.set(Bootstrap.class, this);
		ctx.set(ConnectionManager.class, new ConnectionManager());
		ctx.set(AttachConnector.class, new AttachConnector());

		ctx.startComponents();

		ListenableFuture<Overlay> overlayConnFut = new OverlayConnector(ctx).connectToOverlay(!isClientMode);

		return overlayConnFut;
	}

	public boolean isClientMode() {
		return isClientMode;
	}

	/**
	 * Used to compare overlay instances
	 */
	@Override
	public abstract boolean equals(Object obj);

	/**
	 * Used to map overlay instances
	 */
	@Override
	public abstract int hashCode();

	public NodeID getLocalNodeId() {
		return localNodeId;
	}

	protected void registerComponents() {

	}
}
