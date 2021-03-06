/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.rtp.translator;

import javax.media.protocol.PushSourceStream;

/**
 * Describes a <tt>PushSourceStream</tt> associated with an endpoint from which
 * an <tt>RTPTranslatorImpl</tt> is translating.
 * 
 * @author Lyubomir Marinov
 */
class PushSourceStreamDesc {
	/**
	 * The endpoint <tt>RTPConnector</tt> which owns {@link #stream}.
	 */
	public final RTPConnectorDesc connectorDesc;

	/**
	 * <tt>true</tt> if this instance represents a data/RTP stream or
	 * <tt>false</tt> if this instance represents a control/RTCP stream
	 */
	public final boolean data;

	/**
	 * The <tt>PushSourceStream</tt> associated with an endpoint from which an
	 * <tt>RTPTranslatorImpl</tt> is translating.
	 */
	public final PushSourceStream stream;

	/**
	 * Initializes a new <tt>PushSourceStreamDesc</tt> instance which is to
	 * describe a specific endpoint <tt>PushSourceStream</tt> for an
	 * <tt>RTPTranslatorImpl</tt>.
	 * 
	 * @param connectorDesc
	 *            the endpoint <tt>RTPConnector</tt> which owns the specified
	 *            <tt>stream</tt>
	 * @param stream
	 *            the endpoint <tt>PushSourceStream</tt> to be described by the
	 *            new instance for an <tt>RTPTranslatorImpl</tt>
	 * @param data
	 *            <tt>true</tt> if the specified <tt>stream</tt> is a data/RTP
	 *            stream or <tt>false</tt> if the specified <tt>stream</tt> is a
	 *            control/RTCP stream
	 */
	public PushSourceStreamDesc(RTPConnectorDesc connectorDesc, PushSourceStream stream, boolean data) {
		this.connectorDesc = connectorDesc;
		this.stream = stream;
		this.data = data;
	}
}
