package com.medicaltrust;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyFFT {

  enum WindowFun {
    None, Hamming, Blackman, Hann
  };
  
  // presision
  final int mN;

  final double[] mWn; // for rotatry factor
  final int[] mBr; // for bit reverse
  final double[] mWindow; // for window function

  // for input
  final double[] mXr;

  // for output
  final double[] mYr; // real
  final double[] mYi; // imag

  public MyFFT (int n) {
    mN = n;
    
    mWn = new double[mN];
    mBr = new int[mN];
    mWindow = new double[mN];
    
    mXr = new double[mN];
    
    mYr = new double[mN];
    mYi = new double[mN];

    double arg = 2 * Math.PI / mN;
    
    for (int i = 0; i < mN; i++) {
      mBr[i] = 0;
      mWn[i] = Math.cos(arg * i); // rotary factor table
    }
    
    // make bit reverse table 
    int nHalf = mN / 2;
    for (int i = 1; i < mN ; i <<= 1) {
      for (int j = 0; j < i; j++) mBr[i+j] = mBr[j] + nHalf;
      nHalf >>= 1;
    }

    initWindowTable(WindowFun.None, mWindow, mN);
  }

  public boolean put (double[] in) {
    int i;
    for (i = 0; i < in.length && i < mN; i++) mXr[mN-i-1] = in[i];
    while (i < mN) { mXr[mN-i-1] = 0.0; i++; }
    return true;
  }

  public void getASpectrum (double[] io) {
    put(io);
    fft_time(mYr, mYi);
    double max = Double.MIN_VALUE;
    for (int i = 0; i < mN; i++) {
      final double tr = mYr[i];
      final double ti = mYi[i];
      final double pw = Math.sqrt(tr*tr + ti*ti);
      mYr[i] = pw;
      if (pw > max) max = pw;
    }
    for (int i = 0; i < io.length && i < mN; i++)
      io[i] = mYr[i] / max;
  }

  public void getDFTSpectrum (double[] io) {
    put(io);
    dft_time(mYr, mYi);
    double max = Double.MIN_VALUE;
    for (int i = 0; i < mN; i++) {
      final double tr = mYr[i];
      final double ti = mYi[i];
      final double pw = Math.sqrt(tr*tr + ti*ti);
      mYr[i] = pw;
      if (pw > max) max = pw;
    }
    for (int i = 0; i < io.length && i < mN; i++)
      io[i] = mYr[i] / max;
  }

  public void setWndFnc (WindowFun wf){
    initWindowTable(wf, mWindow, mN);
  }
  
  private static void initWindowTable (WindowFun wf, double[] w, int n) {
    switch (wf) {
    case Hamming:
      for (int i = 0; i < n; i++)
        w[i] = 0.54
             - 0.46 * Math.cos(2.0 * Math.PI * i / (n - 1));
      break;
    case Blackman:
      for (int i = 0; i < n; i++)
        w[i] = 0.42
             - 0.50 * Math.cos(2.0 * Math.PI * i / (n - 1))
             + 0.08 * Math.cos(4.0 * Math.PI * i / (n - 1));
      break;
    case Hann:
      for (int i = 0; i < n; i++)
        w[i] = 0.50 
             - 0.50 * Math.cos(2 * Math.PI * i / (n - 1));
      break;
    default:
      for (int i = 0; i < n; i++)
        w[i] = 1.0;
    } // switch
  }
  
  // descrete fourier transform
  private void dft_time (double[] re, double[] im) {
    for (int i = 0; i < mN; i++) {
      double tr = 0.0;
      double ti = 0.0;
      for (int j = 0; j < mN; j++) {
        final double theta = 2.0 * Math.PI * 0.5 * i * j / mN;
        final double sin = Math.sin(theta);
        final double cos = Math.cos(theta);
        tr += mXr[j] * cos - mXr[j] * sin;
        ti += mXr[j] * sin + mXr[j] * cos;
      }
      mYr[i] = tr / mN;
      mYi[i] = ti / mN;
    }
  }

  // fast fourier transform
  private void fft_time (double[] re, double[] im) {
    fft(re, im);
    return;
    /*
    for (int i = 0; i < mN; i++) {
      re[i] = mXr[i];
      im[i] = 0.0;
    }
    applyWindow(mYr);

    // reverse
    for (int j = 0 ; j < mN; j++) {
      final int k = mBr[j];
      if (j < k) {
        final double tr = re[j];
        final double ti = im[j];
        re[j] = re[k];
        im[j] = im[k];
        re[k] = tr;
        im[k] = ti;
      }
    }
		
    for (int step = (mN>>1); step >= 1; step >>= 1) {
      int nHalf  = 1, nHalf2 = 2;
      
      for (int k = 0; k < mN; k += nHalf2) {
        int jxC = 0;
        
        for (int j = k ; j < (k+nHalf); j++) {
          final int h = j + nHalf;
          
          double tr = re[h];
          double ti = im[h];

          // re[h] = tr * mWn[jxC] + ti * mWn[jxC+mN/4];
          // im[h] = ti * mWn[jxC] - tr * mWn[jxC+mN/4];

          final double arg = 2.0 * Math.PI / mN;
          re[h] = tr*Math.cos(arg*jxC) + ti*Math.sin(arg*jxC); 
          im[h] = ti*Math.cos(arg*jxC) - tr*Math.sin(arg*jxC);

          tr = re[j];
          ti = im[j];

          re[j] = tr + re[h];
          im[j] = ti + im[h];
          
          re[h] = tr - re[h];
          im[h] = ti - im[h];
					
          jxC += step;
        } // for j
      } // for k
      nHalf <<= 1;
      nHalf2 <<= 1;
    } // for step
*/
  }

  private void applyWindow (double[] array) {
    for (int i = 0; i < mN; i++) array[i] *= mWindow[i];
  }


  // this should be replaced by your own fft function!!!
  public void fft (double[] re, double[] im) {
    int n = mN;
    
    double ld = Math.log(n) / Math.log(2.0);
    if (((int) ld) - ld != 0) {
      System.out.println("The number of elements is not a power of 2.");
      return;
    }
 
    // Declaration and initialization of the variables
    // ld should be an integer, actually, so I don't lose any information in
    // the cast
    int nu = (int) ld;
    int n2 = n / 2;
    int nu1 = nu - 1;
    double[] xReal = new double[n];
    double[] xImag = new double[n];
    double tReal, tImag, p, arg, c, s;
    
    double constant= -2 * Math.PI;
    
    for (int i = 0; i < n; i++) {
      xReal[i] = mXr[i];
      xImag[i] = 0.0;
    }
    
    // First phase - calculation
    int k = 0;
    for (int l = 1; l <= nu; l++) {
      while (k < n) {
        for (int i = 1; i <= n2; i++) {
          p = bitreverseReference(k >> nu1, nu);
          // direct FFT or inverse FFT
          arg = constant * p / n;
          c = Math.cos(arg);
          s = Math.sin(arg);
          tReal = xReal[k + n2] * c + xImag[k + n2] * s;
          tImag = xImag[k + n2] * c - xReal[k + n2] * s;
          xReal[k + n2] = xReal[k] - tReal;
          xImag[k + n2] = xImag[k] - tImag;
          xReal[k] += tReal;
          xImag[k] += tImag;
          k++;
        }
        k += n2;
      }
      k = 0;
      nu1--;
      n2 /= 2;
    }
    
    // Second phase - recombination
    k = 0;
    int r;
    while (k < n) {
      r = bitreverseReference(k, nu);
      if (r > k) {
        tReal = xReal[k];
        tImag = xImag[k];
        xReal[k] = xReal[r];
        xImag[k] = xImag[r];
        xReal[r] = tReal;
        xImag[r] = tImag;
      }
      k++;
    }
    
    double radice = 1.0 / Math.sqrt(n);
    for (int i = 0; i < mN; i++) {
      re[i] = xReal[i/2] * radice;
      im[i] = xImag[i/2] * radice;
    }
  }
  
  /**
   * The reference bitreverse function.
   */
  private static int bitreverseReference(int j, int nu) {
    int j2;
    int j1 = j;
    int k = 0;
    for (int i = 1; i <= nu; i++) {
      j2 = j1 / 2;
      k = 2 * k + j1 - 2 * j2;
      j1 = j2;
    }
    return k;
  }
}
