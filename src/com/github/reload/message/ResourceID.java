package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.ResourceID.ResourceIDCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * The identifier of a resource
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
@ReloadCodec(ResourceIDCodec.class)
public final class ResourceID extends RoutableID {

	private final byte[] id;

	private ResourceID(byte[] id) {
		this.id = id;
	}

	public static ResourceID valueOf(byte[] id) {
		return new ResourceID(id);
	}

	public static ResourceID valueOf(String hexString) {
		return new ResourceID(hexToByte(hexString));
	}

	@Override
	public byte[] getData() {
		return id;
	}

	@Override
	public DestinationType getType() {
		return DestinationType.RESOURCEID;
	}

	public static class ResourceIDCodec extends Codec<ResourceID> {

		public ResourceIDCodec(Context context) {
			super(context);
		}

		private static final int VALUE_LENGTH_FIELD = U_INT8;

		@Override
		public void encode(ResourceID obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, VALUE_LENGTH_FIELD);
			buf.writeBytes(obj.id);
			lenFld.updateDataLength();
		}

		@Override
		public ResourceID decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf data = readField(buf, VALUE_LENGTH_FIELD);
			byte[] id = new byte[data.readableBytes()];
			buf.readBytes(id);
			return valueOf(id);
		}
	}
}
