/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.transform.srtp;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jitsi.util.Logger;

/**
 * Adapts the <tt>javax.crypto.Cipher</tt> class to the
 * <tt>org.bouncycastle.crypto.BlockCipher</tt> interface.
 * 
 * @author Lyubomir Marinov
 */
public class BlockCipherAdapter implements BlockCipher {
	/**
	 * The <tt>Logger</tt> used by the <tt>BlockCipherAdapter</tt> class and its
	 * instance to print out debug information.
	 */
	private static final Logger logger = Logger.getLogger(BlockCipherAdapter.class);

	/**
	 * The name of the algorithm implemented by this instance.
	 */
	private final String algorithmName;

	/**
	 * The block size in bytes of this cipher.
	 */
	private final int blockSize;

	/**
	 * The <tt>javax.crypto.Cipher</tt> instance which is adapted to the
	 * <tt>org.bouncycastle.crypto.BlockCipher</tt> interface by this instance.
	 */
	private final Cipher cipher;

	/**
	 * Initializes a new <tt>BlockCipherAdapter</tt> instance which is to adapt
	 * a specific <tt>javax.crypto.Cipher</tt> instance to the
	 * <tt>org.bouncycastle.crypto.BlockCipher</tt> interface.
	 * 
	 * @param cipher
	 *            the <tt>javax.crypto.Cipher</tt> instance to be adapted to the
	 *            <tt>org.bouncycastle.crypto.BlockCipher</tt> interface by the
	 *            new instance
	 */
	public BlockCipherAdapter(Cipher cipher) {
		if (cipher == null)
			throw new NullPointerException("cipher");

		this.cipher = cipher;

		// The value of the algorithm property of javax.crypto.Cipher is a
		// transformation i.e. it may contain mode and padding. However, the
		// algorithm name alone is necessary elsewhere.
		String algorithmName = cipher.getAlgorithm();

		if (algorithmName != null) {
			int endIndex = algorithmName.indexOf('/');

			if (endIndex > 0)
				algorithmName = algorithmName.substring(0, endIndex);

			int len = algorithmName.length();

			if ((len > 4) && (algorithmName.endsWith("_128") || algorithmName.endsWith("_192") || algorithmName.endsWith("_256"))) {
				algorithmName = algorithmName.substring(0, len - 4);
			}
		}
		this.algorithmName = algorithmName;

		this.blockSize = cipher.getBlockSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAlgorithmName() {
		return algorithmName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getBlockSize() {
		return blockSize;
	}

	/**
	 * Gets the <tt>javax.crypto.Cipher</tt> instance which is adapted to the
	 * <tt>org.bouncycastle.crypto.BlockCipher</tt> interface by this instance.
	 * 
	 * @return the <tt>javax.crypto.Cipher</tt> instance which is adapted to the
	 *         <tt>org.bouncycastle.crypto.BlockCipher</tt> interface by this
	 *         instance
	 */
	public Cipher getCipher() {
		return cipher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
		Key key = null;

		if (params instanceof KeyParameter) {
			byte[] bytes = ((KeyParameter) params).getKey();

			if (bytes != null)
				key = new SecretKeySpec(bytes, getAlgorithmName());
		}

		try {
			cipher.init(forEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
		} catch (InvalidKeyException ike) {
			logger.error(ike, ike);
			throw new IllegalArgumentException(ike);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int processBlock(byte[] in, int inOff, byte[] out, int outOff) throws DataLengthException, IllegalStateException {
		try {
			return cipher.update(in, inOff, getBlockSize(), out, outOff);
		} catch (ShortBufferException sbe) {
			logger.error(sbe, sbe);

			DataLengthException dle = new DataLengthException();

			dle.initCause(sbe);
			throw dle;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		try {
			cipher.doFinal();
		} catch (GeneralSecurityException gse) {
			logger.error(gse, gse);
		}
	}
}
