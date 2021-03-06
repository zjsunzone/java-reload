package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.local.StoredData;
import com.github.reload.services.storage.net.FetchKindResponse.FetchKindResponseCodec;

/**
 * A response contained in a fetch answer, contains all the data for a specific
 * kind that matches the request data specifier
 * 
 */
@ReloadCodec(FetchKindResponseCodec.class)
public class FetchKindResponse {

	private final DataKind kind;
	private final BigInteger generation;
	private final List<StoredData> values;

	public FetchKindResponse(DataKind kind, BigInteger generation, List<StoredData> values) {
		this.kind = kind;
		this.generation = generation;
		this.values = values;
	}

	public DataKind getKind() {
		return kind;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public List<StoredData> getValues() {
		return values;
	}

	static class FetchKindResponseCodec extends Codec<FetchKindResponse> {

		private final static int GENERATION_FIELD = U_INT64;
		private final static int VALUES_LENGTH_FIELD = U_INT32;

		private final Codec<DataKind> dataKindCodec;
		private final Codec<StoredData> storedDataCodec;

		public FetchKindResponseCodec(ObjectGraph ctx) {
			super(ctx);
			dataKindCodec = getCodec(DataKind.class);
			storedDataCodec = getCodec(StoredData.class);
		}

		@Override
		public void encode(FetchKindResponse obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			dataKindCodec.encode(obj.kind, buf);

			byte[] genBytes = toUnsigned(obj.generation);

			// Make sure the field is of a fixed size by padding with zeros
			buf.writeZero(GENERATION_FIELD - genBytes.length);

			buf.writeBytes(genBytes);

			encodeData(obj, buf);
		}

		private void encodeData(FetchKindResponse obj, ByteBuf buf) throws com.github.reload.net.codecs.Codec.CodecException {
			Field dataFld = allocateField(buf, VALUES_LENGTH_FIELD);
			for (StoredData d : obj.values) {
				storedDataCodec.encode(d, buf);
			}
			dataFld.updateDataLength();
		}

		@Override
		public FetchKindResponse decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			DataKind kind = dataKindCodec.decode(buf);

			byte[] genData = new byte[GENERATION_FIELD];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			List<StoredData> values = decodeData(kind, buf);
			return new FetchKindResponse(kind, generation, values);
		}

		private List<StoredData> decodeData(DataKind kind, ByteBuf buf) throws CodecException {
			ByteBuf respData = readField(buf, VALUES_LENGTH_FIELD);

			List<StoredData> out = new ArrayList<StoredData>();

			DataModel dataModel = kind.getDataModel();

			while (respData.readableBytes() > 0) {
				StoredData data = storedDataCodec.decode(respData, dataModel);
				out.add(data);
			}
			respData.release();

			return out;
		}

	}
}
