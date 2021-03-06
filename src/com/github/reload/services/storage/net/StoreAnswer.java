package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.services.storage.net.StoreAnswer.StoreAnswerCodec;

@ReloadCodec(StoreAnswerCodec.class)
public class StoreAnswer extends Content {

	private final List<StoreKindResponse> responses;

	public StoreAnswer(List<StoreKindResponse> responses) {
		this.responses = responses;
	}

	public List<StoreKindResponse> getResponses() {
		return responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.STORE_ANS;
	}

	static class StoreAnswerCodec extends Codec<StoreAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT16;

		private final Codec<StoreKindResponse> storeRespCodec;

		public StoreAnswerCodec(ObjectGraph ctx) {
			super(ctx);
			storeRespCodec = getCodec(StoreKindResponse.class);
		}

		@Override
		public void encode(StoreAnswer obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (StoreKindResponse r : obj.responses) {
				storeRespCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreAnswer decode(ByteBuf buf, Object... params) throws CodecException {
			List<StoreKindResponse> responses = new ArrayList<StoreKindResponse>();

			ByteBuf respData = readField(buf, RESPONSES_LENGTH_FIELD);

			while (respData.readableBytes() > 0) {
				responses.add(storeRespCodec.decode(respData));
			}

			respData.release();

			return new StoreAnswer(responses);
		}

	}

}
