package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.net.ice.RelayCandidate.RelayCandidateCodec;

@ReloadCodec(RelayCandidateCodec.class)
public class RelayCandidate extends IceCandidate {

	private IPAddressPort relayAddrPort;

	public RelayCandidate(IPAddressPort addrPort) {
		relayAddrPort = addrPort;
	}

	public void setRelayAddrPort(InetSocketAddress relayAddrPort) {
		this.relayAddrPort = IPAddressPort.create(relayAddrPort);
	}

	public InetSocketAddress getRelayAddrPort() {
		return relayAddrPort;
	}

	@Override
	protected CandidateType getCandType() {
		return CandidateType.RELAY;
	}

	public static class RelayCandidateCodec extends Codec<RelayCandidate> {

		private final Codec<IPAddressPort> sockAddrCodec;

		public RelayCandidateCodec(Configuration conf) {
			super(conf);
			sockAddrCodec = getCodec(IPAddressPort.class);
		}

		@Override
		public void encode(RelayCandidate obj, ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			sockAddrCodec.encode(obj.relayAddrPort, buf);
		}

		@Override
		public RelayCandidate decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			return new RelayCandidate(sockAddrCodec.decode(buf));
		}
	}
}