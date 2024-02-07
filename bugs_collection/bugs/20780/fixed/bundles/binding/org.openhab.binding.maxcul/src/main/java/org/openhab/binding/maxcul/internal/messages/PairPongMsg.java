package org.openhab.binding.maxcul.internal.messages;

public class PairPongMsg extends BaseMsg {

	final static private int PAIR_PONG_PAYLOAD_LEN = 1; /* in bytes */

	public PairPongMsg(byte msgCount, byte msgFlag,
			byte groupId, String srcAddr, String dstAddr) {
		super(msgCount, msgFlag, MaxCulMsgType.PAIR_PONG, groupId, srcAddr, dstAddr);

		byte[] payload = new byte[PAIR_PONG_PAYLOAD_LEN];

		payload[0] = 0x00;

		super.appendPayload(payload);
	}

}
