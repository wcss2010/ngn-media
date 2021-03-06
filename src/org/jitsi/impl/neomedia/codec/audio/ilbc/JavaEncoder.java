/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.audio.ilbc;

import java.awt.Component;
import java.util.Map;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;

import org.jitsi.impl.neomedia.codec.AbstractCodec2;
import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.service.neomedia.control.FormatParametersAwareCodec;

/**
 * Implements an iLBC encoder and RTP packetizer as a {@link Codec}.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class JavaEncoder
    extends AbstractCodec2
    implements FormatParametersAwareCodec
{
    /**
     * The <tt>ilbc_encoder</tt> adapted to <tt>Codec</tt> by this instance.
     */
    private ilbc_encoder enc = null;

    /**
     * The input length in bytes with which {@link #enc} has been initialized.
     */
    private int inputLength;

    /**
     * The output length in bytes with which {@link #enc} has been initialized.
     */
    private int outputLength;

    /**
     * The input from previous calls to {@link #doProcess(Buffer, Buffer)} which
     * has not been consumed yet.
     */
    private byte[] prevInput;

    /**
     * The number of bytes in {@link #prevInput} which have not been consumed
     * yet.
     */
    private int prevInputLength;

    /**
     * The duration an output <tt>Buffer</tt> produced by this <tt>Codec</tt>.
     */
    private int duration = 0;

    /**
     * Initializes a new iLBC <tt>JavaEncoder</tt> instance.
     */
    public JavaEncoder()
    {
        super(
                "iLBC Encoder",
                AudioFormat.class,
                new Format[]
                        {
                            new AudioFormat(
                            		AudioFormat.ILBC_RTP,
                                    8000,
                                    16,
                                    1,
                                    AudioFormat.LITTLE_ENDIAN,
                                    AudioFormat.SIGNED)
                        });

        inputFormats
            = new Format[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                8000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED)
                    };

        addControl(
                new com.sun.media.controls.SilenceSuppressionAdapter(
                        this,
                        false,
                        false));

        addControl(this);
    }

    /**
     * Implements {@link AbstractCodec2#doClose()}.
     *
     * @see AbstractCodec2#doClose()
     */
    @Override
    protected void doClose()
    {
        enc = null;
        outputLength = 0;
        inputLength = 0;
        prevInput = null;
        prevInputLength = 0;
        duration = 0;
    }

    /**
     * Implements {@link AbstractCodec2#doOpen()}.
     *
     * @see AbstractCodec2#doOpen()
     */
    @Override
    protected void doOpen()
    {
        // if not already initialised, use the constant for default value (30)
        if(enc == null)
            initEncoder(Constants.ILBC_MODE);
    }

    /**
     * Init encoder with specified mode.
     * @param mode the mode to use.
     */
    private void initEncoder(int mode)
    {
        enc = new ilbc_encoder(mode);

        switch (mode)
        {
            case 20:
                outputLength = ilbc_constants.NO_OF_BYTES_20MS;
                break;
            case 30:
                outputLength = ilbc_constants.NO_OF_BYTES_30MS;
                break;
            default:
                throw new IllegalStateException("mode");
        }
        /* mode is 20 or 30 ms, duration must be in nanoseconds */
        duration = mode * 1000000;
        inputLength = enc.ULP_inst.blockl * 2;
        prevInput = new byte[inputLength];
        prevInputLength = 0;
    }

    /**
     * Get the output format.
     *
     * @return output format
     * @see net.sf.fmj.media.AbstractCodec#getOutputFormat()
     */
    @Override
    public Format getOutputFormat()
    {
        Format outputFormat = super.getOutputFormat();

        if ((outputFormat != null)
                && (outputFormat.getClass() == AudioFormat.class))
        {
            AudioFormat outputAudioFormat = (AudioFormat) outputFormat;

            outputFormat = setOutputFormat(
                new AudioFormat(
                            outputAudioFormat.getEncoding(),
                            outputAudioFormat.getSampleRate(),
                            outputAudioFormat.getSampleSizeInBits(),
                            outputAudioFormat.getChannels(),
                            outputAudioFormat.getEndian(),
                            outputAudioFormat.getSigned(),
                            outputAudioFormat.getFrameSizeInBits(),
                            outputAudioFormat.getFrameRate(),
                            outputAudioFormat.getDataType())
                        {
                            private static final long serialVersionUID = 0L;

                            @Override
                            public long computeDuration(long length)
                            {
                                return JavaEncoder.this.duration;
                            }
                        });
        }
        return outputFormat;
    }

    /**
     * Implements {@link AbstractCodec2#doProcess(Buffer, Buffer)}.
     *
     * @param inputBuffer the input buffer
     * @param outputBuffer the output buffer
     * @return the status of the processing, whether buffer is consumed/filled..
     * @see AbstractCodec2#doProcess(Buffer, Buffer)
     */
    @Override
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        int inputLength = inputBuffer.getLength();
        byte[] input = (byte[]) inputBuffer.getData();
        int inputOffset = inputBuffer.getOffset();

        if ((prevInputLength != 0) || (inputLength < this.inputLength))
        {
            int bytesToCopy = this.inputLength - prevInputLength;

            if (bytesToCopy > inputLength)
                bytesToCopy = inputLength;
            System.arraycopy(
                    input, inputOffset,
                    prevInput, prevInputLength,
                    bytesToCopy);
            prevInputLength += bytesToCopy;

            inputBuffer.setLength(inputLength - bytesToCopy);
            inputBuffer.setOffset(inputOffset + bytesToCopy);

            inputLength = prevInputLength;
            input = prevInput;
            inputOffset = 0;
        }
        else
        {
            inputBuffer.setLength(inputLength - this.inputLength);
            inputBuffer.setOffset(inputOffset + this.inputLength);
        }

        int ret;

        if (inputLength >= this.inputLength)
        {
            /*
             * If we are about to encode from prevInput, we already have
             * prevInputLength taken into consideration by using prevInput in
             * the first place and we have to make sure that we will not use the
             * same prevInput more than once.
             */
            prevInputLength = 0;

            int outputOffset = 0;
            byte[] output
                = validateByteArraySize(
                        outputBuffer,
                        outputOffset + outputLength,
                        true);

            enc.encode(output, outputOffset, input, inputOffset);

            updateOutput(
                    outputBuffer,
                    getOutputFormat(), outputLength, outputOffset);
            outputBuffer.setDuration(duration);
            ret = BUFFER_PROCESSED_OK;
        }
        else
            ret = OUTPUT_BUFFER_NOT_FILLED;

        if (inputBuffer.getLength() > 0)
            ret |= INPUT_BUFFER_NOT_CONSUMED;
        return ret;
    }

    /**
     * Sets the format parameters to <tt>fmtps</tt>
     *
     * @param fmtps The format parameters to set
     */
    @Override
    public void setFormatParameters(Map<String, String> fmtps)
    {
        String modeStr = fmtps.get("mode");

        if(modeStr != null)
        {
            try
            {
                int mode = Integer.valueOf(modeStr);

                // supports only mode 20 or 30
                if(mode == 20 || mode == 30)
                    initEncoder(mode);
            }
            catch(Throwable t){}
        }
    }

    /**
     * Implements {@link javax.media.Control#getControlComponent()}.
     */
    @Override
    public Component getControlComponent()
    {
        return null;
    }
}
