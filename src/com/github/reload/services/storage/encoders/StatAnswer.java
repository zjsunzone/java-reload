package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.services.storage.encoders.StatAnswer.StatAnswerCodec;

@ReloadCodec(StatAnswerCodec.class)
public class StatAnswer extends Content {

	private final List<StatKindResponse> responses;

	public StatAnswer(List<StatKindResponse> responses) {
		this.responses = responses;
	}

	public List<StatKindResponse> getResponses() {
		return responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_ANS;
	}

	static class StatAnswerCodec extends Codec<StatAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT32;

		private final Codec<StatKindResponse> respCodec;

		public StatAnswerCodec(ComponentsContext ctx) {
			super(ctx);
			respCodec = getCodec(StatKindResponse.class);
		}

		@Override
		public void encode(StatAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (StatKindResponse r : obj.responses) {
				respCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StatAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			ByteBuf resposeData = readField(buf, RESPONSES_LENGTH_FIELD);
			List<StatKindResponse> responses = new ArrayList<StatKindResponse>();

			while (resposeData.readableBytes() > 0) {
				responses.add(respCodec.decode(resposeData));
			}

			resposeData.release();

			return new StatAnswer(responses);
		}
	}
}