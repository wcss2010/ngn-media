/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.neomedia.rtp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.fmj.media.rtp.RTCPFeedback;
import net.sf.fmj.media.rtp.RTCPReport;

import org.jitsi.util.Logger;

/**
 * Collects the (last) RTCP (SR, RR, and XR) reports sent and received by a
 * local peer (for the purposes of <tt>MediaStreamStats</tt>).
 * 
 * @author Lyubomir Marinov
 */
public class RTCPReports {
	/**
	 * The <tt>Logger</tt> used by the <tt>RTCPReports</tt> class and its
	 * instances to print debug information.
	 */
	private static final Logger logger = Logger.getLogger(RTCPReports.class);

	/**
	 * The list of <tt>RTCPReportListener</tt>s to be notified by this instance
	 * about the receiving and sending of RTCP RR, SR, and XR. Implemented as
	 * copy-on-write storage in order to optimize the firing of events to the
	 * listeners.
	 */
	private List<RTCPReportListener> listeners = Collections.emptyList();

	/**
	 * The <tt>Object</tt> which synchronizes the (write) access to
	 * {@link #listeners}.
	 */
	private final Object listenerSyncRoot = new Object();

	/**
	 * The RTCP extended reports (XR) received by the local endpoint represented
	 * by this instance associated with the synchronization source identifiers
	 * of their respective originator (SSRC defined by RFC 3611).
	 */
	private final Map<Integer, RTCPExtendedReport> receivedExtendedReports = new HashMap<Integer, RTCPExtendedReport>();

	/**
	 * The RTCP sender report (SR) and/or receiver report (RR) blocks received
	 * by the local endpoint represented by this instance associated with the
	 * synchronization source identifiers of their respective source (SSRC of
	 * source defined by RFC 3550).
	 */
	private final Map<Integer, RTCPFeedback> receivedFeedbacks = new HashMap<Integer, RTCPFeedback>();

	/**
	 * The RTCP sender reports (SR) and/or receiver reports (RR) received by the
	 * local endpoint represented by this instance associated with the
	 * synchronization source identifiers of their respective originator (SSRC
	 * of sender defined by RFC 3550).
	 */
	private final Map<Integer, RTCPReport> receivedReports = new HashMap<Integer, RTCPReport>();

	/**
	 * The RTCP extended report (XR) VoIP Metrics blocks received by the local
	 * endpoint represented by this instance associated with the synchronization
	 * source identifiers of their respective source (SSRC of source defined by
	 * RFC 3611).
	 */
	private final Map<Integer, RTCPExtendedReport.VoIPMetricsReportBlock> receivedVoIPMetrics = new HashMap<Integer, RTCPExtendedReport.VoIPMetricsReportBlock>();

	/**
	 * The RTCP extended reports (XR) sent by the local endpoint represented by
	 * this instance associated with the synchronization source identifiers of
	 * their respective originator (SSRC defined by RFC 3611).
	 */
	private final Map<Integer, RTCPExtendedReport> sentExtendedReports = new HashMap<Integer, RTCPExtendedReport>();

	/**
	 * The RTCP sender report (SR) and/or receiver report (RR) blocks sent by
	 * the local endpoint represented by this instance associated with the
	 * synchronization source identifiers of their respective source (SSRC of
	 * source defined by RFC 3550).
	 */
	private final Map<Integer, RTCPFeedback> sentFeedbacks = new HashMap<Integer, RTCPFeedback>();

	/**
	 * The RTCP sender reports (SR) and/or receiver reports (RR) sent by the
	 * local endpoint represented by this instance associated with the
	 * synchronization source identifiers of their respective originator (SSRC
	 * of sender defined by RFC 3550).
	 */
	private final Map<Integer, RTCPReport> sentReports = new HashMap<Integer, RTCPReport>();

	/**
	 * The RTCP extended report (XR) VoIP Metrics blocks sent by the local
	 * endpoint represented by this instance associated with the synchronization
	 * source identifiers of their respective source (SSRC of source defined by
	 * RFC 3611).
	 */
	private final Map<Integer, RTCPExtendedReport.VoIPMetricsReportBlock> sentVoIPMetrics = new HashMap<Integer, RTCPExtendedReport.VoIPMetricsReportBlock>();

	/**
	 * Adds a new <tt>RTCPReportListener</tt> to be notified by this instance
	 * about the receiving and sending of RTCP RR, SR and XR.
	 * 
	 * @param listener
	 *            the <tt>RTCPReportListener</tt> to add
	 * @throws NullPointerException
	 *             if the specified <tt>listener</tt> is <tt>null</tt>
	 */
	public void addRTCPReportListener(RTCPReportListener listener) {
		if (listener == null)
			throw new NullPointerException("listener");

		synchronized (listenerSyncRoot) {
			if (!listeners.contains(listener)) {
				List<RTCPReportListener> newListeners = new ArrayList<RTCPReportListener>(listeners.size() + 1);

				newListeners.addAll(listeners);
				newListeners.add(listener);

				listeners = Collections.unmodifiableList(newListeners);
			}
		}
	}

	/**
	 * Gets the latest RTCP XR received from a specific SSRC (of remote
	 * originator).
	 * 
	 * @param ssrc
	 *            the SSRC of the RTCP XR (remote) originator
	 * @return the latest RTCP XR received from the specified <tt>ssrc</tt>
	 */
	public RTCPExtendedReport getReceivedRTCPExtendedReport(int ssrc) {
		synchronized (receivedExtendedReports) {
			return receivedExtendedReports.get(Integer.valueOf(ssrc));
		}
	}

	/**
	 * Gets the RTCP extended reports (XR) received by the local endpoint.
	 * 
	 * @return the RTCP extended reports (XR) received by the local endpoint
	 */
	public RTCPExtendedReport[] getReceivedRTCPExtendedReports() {
		synchronized (receivedExtendedReports) {
			Collection<RTCPExtendedReport> values = receivedExtendedReports.values();

			return values.toArray(new RTCPExtendedReport[values.size()]);
		}
	}

	/**
	 * Gets the latest RTCP SR or RR report block received from a remote
	 * sender/originator for a local source.
	 * 
	 * @param sourceSSRC
	 *            the SSRC of the local source
	 * @return the latest RTCP SR or RR report block received from a remote
	 *         sender/originator for the specified <tt>sourceSSRC</tt>
	 */
	public RTCPFeedback getReceivedRTCPFeedback(int sourceSSRC) {
		synchronized (receivedReports) {
			return receivedFeedbacks.get(Integer.valueOf(sourceSSRC));
		}
	}

	/**
	 * Gets the RTCP sender report (SR) and/or receiver report (RR) blocks
	 * received by the local endpoint.
	 * 
	 * @return the RTCP sender report (SR) and/or receiver report (RR) blocks
	 *         received by the local endpoint
	 */
	public RTCPFeedback[] getReceivedRTCPFeedbacks() {
		synchronized (receivedReports) {
			Collection<RTCPFeedback> values = receivedFeedbacks.values();

			return values.toArray(new RTCPFeedback[values.size()]);
		}
	}

	/**
	 * Gets the latest RTCP SR or RR received from a specific SSRC (of remote
	 * sender/originator).
	 * 
	 * @param senderSSRC
	 *            the SSRC of the RTCP SR or RR (remote) sender/originator
	 * @return the latest RTCP SR or RR received from the specified
	 *         <tt>senderSSRC</tt>
	 */
	public RTCPReport getReceivedRTCPReport(int senderSSRC) {
		synchronized (receivedReports) {
			return receivedReports.get(Integer.valueOf(senderSSRC));
		}
	}

	/**
	 * Gets the RTCP sender reports (SR) and/or receiver reports (RR) received
	 * by the local endpoint.
	 * 
	 * @return the RTCP sender reports (SR) and/or receiver reports (RR)
	 *         received by the local endpoint
	 */
	public RTCPReport[] getReceivedRTCPReports() {
		synchronized (receivedReports) {
			Collection<RTCPReport> values = receivedReports.values();

			return values.toArray(new RTCPReport[values.size()]);
		}
	}

	/**
	 * Gets the RTCP extended report (XR) VoIP Metrics blocks received by the
	 * local endpoint.
	 * 
	 * @return the RTCP extended report (XR) VoIP Metrics blocks received by the
	 *         local endpoint
	 */
	public RTCPExtendedReport.VoIPMetricsReportBlock[] getReceivedRTCPVoIPMetrics() {
		synchronized (receivedExtendedReports) {
			Collection<RTCPExtendedReport.VoIPMetricsReportBlock> values = receivedVoIPMetrics.values();

			return values.toArray(new RTCPExtendedReport.VoIPMetricsReportBlock[values.size()]);
		}
	}

	/**
	 * Gets the latest RTCP extended report (XR) VoIP Metrics block received
	 * from a remote originator for a local source.
	 * 
	 * @param sourceSSRC
	 *            the SSRC of the local source
	 * @return the RTCP extended report (XR) VoIP Metrics block received from a
	 *         remote originator for the specified <tt>sourceSSRC</tt>
	 */
	public RTCPExtendedReport.VoIPMetricsReportBlock getReceivedRTCPVoIPMetrics(int sourceSSRC) {
		synchronized (receivedExtendedReports) {
			return receivedVoIPMetrics.get(Integer.valueOf(sourceSSRC));
		}
	}

	/**
	 * Gets a list of the <tt>RTCPReportListener</tt>s to be notified by this
	 * instance about the receiving and sending of RTCP RR, SR, and XR.
	 * 
	 * @return a list of the <tt>RTCPReportListener</tt>s to be notified by this
	 *         instance about the receiving and sending of RTCP RR, SR, and XR
	 */
	public List<RTCPReportListener> getRTCPReportListeners() {
		return listeners;
	}

	/**
	 * Gets the latest RTCP XR sent from a specific SSRC (of local originator).
	 * 
	 * @param ssrc
	 *            the SSRC of the RTCP XR (local) originator
	 * @return the latest RTCP XR sent from the specified <tt>ssrc</tt>
	 */
	public RTCPExtendedReport getSentRTCPExtendedReport(int ssrc) {
		synchronized (sentExtendedReports) {
			return sentExtendedReports.get(Integer.valueOf(ssrc));
		}
	}

	/**
	 * Gets the RTCP extended reports (XR) sent by the local endpoint.
	 * 
	 * @return the RTCP extended reports (XR) sent by the local endpoint
	 */
	public RTCPExtendedReport[] getSentRTCPExtendedReports() {
		synchronized (sentExtendedReports) {
			Collection<RTCPExtendedReport> values = sentExtendedReports.values();

			return values.toArray(new RTCPExtendedReport[values.size()]);
		}
	}

	/**
	 * Gets the latest RTCP SR or RR report block sent from a local
	 * sender/originator for a remote source.
	 * 
	 * @param sourceSSRC
	 *            the SSRC of the remote source
	 * @return the latest RTCP SR or RR report block received from a local
	 *         sender/originator for the specified <tt>sourceSSRC</tt>
	 */
	public RTCPFeedback getSentRTCPFeedback(int sourceSSRC) {
		synchronized (sentReports) {
			return sentFeedbacks.get(Integer.valueOf(sourceSSRC));
		}
	}

	/**
	 * Gets the RTCP sender report (SR) and/or receiver report (RR) blocks sent
	 * by the local endpoint.
	 * 
	 * @return the RTCP sender report (SR) and/or receiver report (RR) blocks
	 *         sent by the local endpoint
	 */
	public RTCPFeedback[] getSentRTCPFeedbacks() {
		synchronized (sentReports) {
			Collection<RTCPFeedback> values = sentFeedbacks.values();

			return values.toArray(new RTCPFeedback[values.size()]);
		}
	}

	/**
	 * Gets the latest RTCP SR or RR sent from a specific SSRC (of local
	 * sender/originator).
	 * 
	 * @param senderSSRC
	 *            the SSRC of the RTCP SR or RR (local) sender/originator
	 * @return the latest RTCP SR or RR sent from the specified
	 *         <tt>senderSSRC</tt>
	 */
	public RTCPReport getSentRTCPReport(int senderSSRC) {
		synchronized (sentReports) {
			return sentReports.get(Integer.valueOf(senderSSRC));
		}
	}

	/**
	 * Gets the RTCP sender reports (SR) and/or receiver reports (RR) sent by
	 * the local endpoint.
	 * 
	 * @return the RTCP sender reports (SR) and/or receiver reports (RR) sent by
	 *         the local endpoint
	 */
	public RTCPReport[] getSentRTCPReports() {
		synchronized (sentReports) {
			Collection<RTCPReport> values = sentReports.values();

			return values.toArray(new RTCPReport[values.size()]);
		}
	}

	/**
	 * Gets the RTCP extended report (XR) VoIP Metrics blocks sent by the local
	 * endpoint.
	 * 
	 * @return the RTCP extended report (XR) VoIP Metrics blocks sent by the
	 *         local endpoint
	 */
	public RTCPExtendedReport.VoIPMetricsReportBlock[] getSentRTCPVoIPMetrics() {
		synchronized (sentExtendedReports) {
			Collection<RTCPExtendedReport.VoIPMetricsReportBlock> values = sentVoIPMetrics.values();

			return values.toArray(new RTCPExtendedReport.VoIPMetricsReportBlock[values.size()]);
		}
	}

	/**
	 * Gets the latest RTCP extended report (XR) VoIP Metrics block sent from a
	 * local originator for a remote source.
	 * 
	 * @param sourceSSRC
	 *            the SSRC of the remote source
	 * @return the RTCP extended report (XR) VoIP Metrics block sent from a
	 *         local originator for the specified <tt>sourceSSRC</tt>
	 */
	public RTCPExtendedReport.VoIPMetricsReportBlock getSentRTCPVoIPMetrics(int sourceSSRC) {
		synchronized (sentExtendedReports) {
			return sentVoIPMetrics.get(Integer.valueOf(sourceSSRC));
		}
	}

	/**
	 * Removes an existing <tt>RTCPReportListener</tt> to no longer be notified
	 * by this instance about the receiving and sending of RTCP RR, SR and XR.
	 * 
	 * @param listener
	 *            the <tt>RTCPReportListener</tt> to remove
	 */
	public void removeRTCPReportListener(RTCPReportListener listener) {
		if (listener == null)
			return;

		synchronized (listenerSyncRoot) {
			int index = listeners.indexOf(listener);

			if (index != -1) {
				if (listeners.size() == 1) {
					listeners = Collections.emptyList();
				} else {
					List<RTCPReportListener> newListeners = new ArrayList<RTCPReportListener>(listeners);

					newListeners.remove(index);

					listeners = Collections.unmodifiableList(newListeners);
				}
			}
		}
	}

	/**
	 * Notifies this instance that a specific <tt>RTCPExtendedReport</tt> was
	 * received by the local endpoint. Remembers the received
	 * <tt>extendedReport</tt> and notifies the <tt>RTCPReportListener</tt>s
	 * added to this instance.
	 * 
	 * @param extendedReport
	 *            the received <tt>RTCPExtendedReport</tt>
	 * @throws NullPointerException
	 *             if the specified <tt>extendedReport</tt> is <tt>null</tt>
	 */
	public void rtcpExtendedReportReceived(RTCPExtendedReport extendedReport) {
		if (extendedReport == null)
			throw new NullPointerException("extendedReport");

		boolean fire;

		synchronized (receivedExtendedReports) {
			Object oldValue = receivedExtendedReports.put(Integer.valueOf((int) extendedReport.getSSRC()), extendedReport);

			if (extendedReport.equals(oldValue)) {
				fire = false;
			} else {
				if (extendedReport.getSystemTimeStamp() == 0) {
					extendedReport.setSystemTimeStamp(System.currentTimeMillis());
				}

				// VoIP Metrics Report Block
				for (RTCPExtendedReport.ReportBlock reportBlock : extendedReport.getReportBlocks()) {
					if (reportBlock instanceof RTCPExtendedReport.VoIPMetricsReportBlock) {
						RTCPExtendedReport.VoIPMetricsReportBlock voipMetrics = (RTCPExtendedReport.VoIPMetricsReportBlock) reportBlock;

						receivedVoIPMetrics.put(Integer.valueOf(voipMetrics.getSourceSSRC()), voipMetrics);
					}
				}

				fire = true;
			}
		}

		if (fire) {
			for (RTCPReportListener listener : getRTCPReportListeners())
				listener.rtcpExtendedReportReceived(extendedReport);
		}

		if (logger.isTraceEnabled())
			logger.trace("Received " + extendedReport + ".");
	}

	/**
	 * Notifies this instance that a specific <tt>RTCPExtendedReport</tt> was
	 * sent by the local endpoint. Remembers the sent <tt>extendedReport</tt>
	 * and notifies the <tt>RTCPReportListener</tt>s added to this instance.
	 * 
	 * @param extendedReport
	 *            the sent <tt>RTCPExtendedReport</tt>
	 * @throws NullPointerException
	 *             if the specified <tt>extendedReport</tt> is <tt>null</tt>
	 */
	public void rtcpExtendedReportSent(RTCPExtendedReport extendedReport) {
		if (extendedReport == null)
			throw new NullPointerException("extendedReport");

		boolean fire;

		synchronized (sentExtendedReports) {
			Object oldValue = sentExtendedReports.put(Integer.valueOf((int) extendedReport.getSSRC()), extendedReport);

			if (extendedReport.equals(oldValue)) {
				fire = false;
			} else {
				if (extendedReport.getSystemTimeStamp() == 0) {
					extendedReport.setSystemTimeStamp(System.currentTimeMillis());
				}

				// VoIP Metrics Report Block
				for (RTCPExtendedReport.ReportBlock reportBlock : extendedReport.getReportBlocks()) {
					if (reportBlock instanceof RTCPExtendedReport.VoIPMetricsReportBlock) {
						RTCPExtendedReport.VoIPMetricsReportBlock voipMetrics = (RTCPExtendedReport.VoIPMetricsReportBlock) reportBlock;

						sentVoIPMetrics.put(Integer.valueOf(voipMetrics.getSourceSSRC()), voipMetrics);
					}
				}

				fire = true;
			}
		}

		if (fire) {
			for (RTCPReportListener listener : getRTCPReportListeners())
				listener.rtcpExtendedReportSent(extendedReport);
		}

		if (logger.isTraceEnabled())
			logger.trace("Sent " + extendedReport + ".");
	}

	/**
	 * Notifies this instance that a specific <tt>RTCPReport</tt> was received
	 * by the local endpoint. Remembers the received <tt>report</tt> and
	 * notifies the <tt>RTCPReportListener</tt>s added to this instance.
	 * 
	 * @param report
	 *            the received <tt>RTCPReport</tt>
	 * @throws NullPointerException
	 *             if the specified <tt>report</tt> is <tt>null</tt>
	 */
	public void rtcpReportReceived(RTCPReport report) {
		if (report == null)
			throw new NullPointerException("report");

		boolean fire;

		synchronized (receivedReports) {
			Object oldValue = receivedReports.put(Integer.valueOf((int) report.getSSRC()), report);

			if (report.equals(oldValue)) {
				fire = false;
			} else {
				if (report.getSystemTimeStamp() == 0)
					report.setSystemTimeStamp(System.currentTimeMillis());

				// RTCPFeedback
				@SuppressWarnings("unchecked")
				List<RTCPFeedback> feedbacks = report.getFeedbackReports();

				if (feedbacks != null) {
					for (RTCPFeedback feedback : feedbacks) {
						receivedFeedbacks.put(Integer.valueOf((int) feedback.getSSRC()), feedback);
					}
				}

				fire = true;
			}
		}

		if (fire) {
			for (RTCPReportListener listener : getRTCPReportListeners())
				listener.rtcpReportReceived(report);
		}
	}

	/**
	 * Notifies this instance that a specific <tt>RTCPReport</tt> was sent by
	 * the local endpoint. Remembers the sent <tt>report</tt> and notifies the
	 * <tt>RTCPReportListener</tt>s added to this instance.
	 * 
	 * @param report
	 *            the sent <tt>RTCPReport</tt>
	 * @throws NullPointerException
	 *             if the specified <tt>report</tt> is <tt>null</tt>
	 */
	public void rtcpReportSent(RTCPReport report) {
		if (report == null)
			throw new NullPointerException("report");

		boolean fire;

		synchronized (sentReports) {
			Object oldValue = sentReports.put(Integer.valueOf((int) report.getSSRC()), report);

			if (report.equals(oldValue)) {
				fire = false;
			} else {
				if (report.getSystemTimeStamp() == 0)
					report.setSystemTimeStamp(System.currentTimeMillis());

				// RTCPFeedback
				@SuppressWarnings("unchecked")
				List<RTCPFeedback> feedbacks = report.getFeedbackReports();

				if (feedbacks != null) {
					for (RTCPFeedback feedback : feedbacks) {
						sentFeedbacks.put(Integer.valueOf((int) feedback.getSSRC()), feedback);
					}
				}

				fire = true;
			}
		}

		if (fire) {
			for (RTCPReportListener listener : getRTCPReportListeners())
				listener.rtcpReportSent(report);
		}
	}
}
