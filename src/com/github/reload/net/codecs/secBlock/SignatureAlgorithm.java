package com.github.reload.net.codecs.secBlock;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.secBlock.SignatureAlgorithm.SignatureAlgorithmCodec;

@ReloadCodec(SignatureAlgorithmCodec.class)
public enum SignatureAlgorithm {
	ANONYMOUS((byte) 0x00, "ANONYMOUS"),
	RSA((byte) 0x01, "RSA"),
	DSA((byte) 0x02, "DSA"),
	ECDSA((byte) 0x03, "ECDSA");

	private final byte code;
	private final String asString;

	private SignatureAlgorithm(byte code, String asString) {
		this.code = code;
		this.asString = asString;
	}

	public byte getCode() {
		return code;
	}

	public static SignatureAlgorithm valueOf(byte code) {
		for (SignatureAlgorithm a : EnumSet.allOf(SignatureAlgorithm.class))
			if (a.code == code)
				return a;
		return null;
	}

	public static SignatureAlgorithm getFromString(String v) {
		for (SignatureAlgorithm a : EnumSet.allOf(SignatureAlgorithm.class))
			if (a.asString.equalsIgnoreCase(v))
				return a;
		return null;
	}

	static class SignatureAlgorithmCodec extends Codec<SignatureAlgorithm> {

		public SignatureAlgorithmCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(SignatureAlgorithm obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.code);
		}

		@Override
		public SignatureAlgorithm decode(ByteBuf buf, Object... params) throws CodecException {
			SignatureAlgorithm signAlg = SignatureAlgorithm.valueOf(buf.readByte());

			if (signAlg == null)
				throw new CodecException("Unsupported signature algorithm");
			return signAlg;
		}

	}

	@Override
	public String toString() {
		return asString;
	}
}