package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.UpdateAnswer.UpdateAnswerCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(UpdateAnswerCodec.class)
public class UpdateAnswer extends Content {

	private final byte[] overlayData;

	public UpdateAnswer(byte[] overlayData) {
		this.overlayData = overlayData;
	}

	public byte[] getOverlayData() {
		return overlayData;
	}

	@Override
	public final ContentType getType() {
		return ContentType.UPDATE_ANS;
	}

	public static class UpdateAnswerCodec extends Codec<UpdateAnswer> {

		public UpdateAnswerCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(UpdateAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			buf.writeBytes(obj.overlayData);
		}

		@Override
		public UpdateAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			return new UpdateAnswer(data);
		}

	}

}
