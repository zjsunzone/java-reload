package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.encoders.Codec.CodecException;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.secBlock.SecurityBlock;

/**
 * Codec for message payload (content + security block)
 */
public class MessagePayloadDecoder extends MessageToMessageDecoder<ForwardMessage> {

	private final ComponentsContext compCtx;
	private final Codec<Content> contentCodec;
	private final Codec<SecurityBlock> secBlockCodec;

	public MessagePayloadDecoder(ComponentsContext ctx) {
		compCtx = ctx;
		contentCodec = Codec.getCodec(Content.class, ctx);
		secBlockCodec = Codec.getCodec(SecurityBlock.class, ctx);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ForwardMessage msg, List<Object> out) throws Exception {
		Header header = msg.getHeader();
		ByteBuf payload = msg.getPayload();
		try {
			int contentStart = payload.readerIndex();

			try {
				Content content = contentCodec.decode(payload);

				ByteBuf rawContent = payload.copy(contentStart, payload.readerIndex() - contentStart);

				SecurityBlock secBlock = secBlockCodec.decode(payload);

				Message outMsg = new Message(header, content, secBlock);

				header.setAttribute(Header.RAW_CONTENT, rawContent);

				out.add(outMsg);
				Logger.getRootLogger().trace(String.format("Message payload %#x decoded", header.getTransactionId()));
			} catch (CodecException e) {
				if (e instanceof ErrorMessageException) {
					ErrorMessageException error = (ErrorMessageException) e;
					Error content = new Error(error.getType(), error.getInfo());
					compCtx.get(MessageRouter.class).sendAnswer(header, content);
					Logger.getRootLogger().debug(String.format("Sent error message caused by decoding of %#x: %s", header.getTransactionId(), e.getMessage()));
				} else {
					Logger.getRootLogger().warn(String.format("Message payload %#x decoding failed: %s", header.getTransactionId(), e.getMessage()));
				}
			}
		} finally {
			payload.release();
		}
	}
}