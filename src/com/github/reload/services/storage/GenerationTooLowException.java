package com.github.reload.services.storage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Header;
import com.github.reload.net.codecs.Codec.CodecException;
import com.github.reload.net.codecs.content.Error.ErrorMessageException;
import com.github.reload.net.codecs.content.Error.ErrorType;
import com.github.reload.services.storage.net.StoreAnswer;

/**
 * Indicates that a store request has the generation value lower than the stored
 * generation value. Contains a store answer with the stored generation value.
 * 
 */
public class GenerationTooLowException extends ErrorMessageException {

	private final StoreAnswer answer;

	public GenerationTooLowException(StoreAnswer answer) {
		super(ErrorType.GEN_COUNTER_TOO_LOW, getEncodedAnswer(answer));
		this.answer = answer;
	}

	public GenerationTooLowException(Header reqHeader, byte[] answer) {
		super(ErrorType.GEN_COUNTER_TOO_LOW, answer);
		this.answer = getDecodedAnswer(answer);
	}

	public StoreAnswer getAnswer() {
		return answer;
	}

	private static byte[] getEncodedAnswer(StoreAnswer answer) {
		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer();
		Codec<StoreAnswer> codec = Codec.getCodec(StoreAnswer.class, null);
		try {
			codec.encode(answer, buf);
		} catch (CodecException e) {
			throw new RuntimeException(e);
		}

		byte[] out = new byte[buf.readableBytes()];
		buf.readBytes(out);
		buf.release();

		return out;

	}

	private static StoreAnswer getDecodedAnswer(byte[] answer) {
		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer();
		buf.writeBytes(answer);

		Codec<StoreAnswer> codec = Codec.getCodec(StoreAnswer.class, null);
		try {
			StoreAnswer out = codec.decode(buf);
			buf.release();
			return out;
		} catch (CodecException e) {
			throw new RuntimeException(e);
		}
	}
}
