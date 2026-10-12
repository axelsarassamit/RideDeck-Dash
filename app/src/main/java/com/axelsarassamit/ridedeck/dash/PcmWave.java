package com.axelsarassamit.ridedeck.dash;
import java.io.*;import java.nio.*;
final class PcmWave implements AutoCloseable {
    final RandomAccessFile file;final int rate,channels;long bytes;
    PcmWave(File path,int sampleRate,int channelCount)throws IOException{rate=sampleRate;channels=channelCount;file=new RandomAccessFile(path,"rw");file.setLength(0);header();}
    void append(short[] samples,int count)throws IOException{ByteBuffer buffer=ByteBuffer.allocate(count*2).order(ByteOrder.LITTLE_ENDIAN);for(int i=0;i<count;i++)buffer.putShort(samples[i]);file.write(buffer.array());bytes+=count*2L;}
    void header()throws IOException{file.seek(0);ByteBuffer b=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt((int)(36+bytes)).put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(16).putShort((short)1).putShort((short)channels).putInt(rate).putInt(rate*channels*2).putShort((short)(channels*2)).putShort((short)16).put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt((int)bytes);file.write(b.array());}
    @Override public void close()throws IOException{try{header();}finally{file.close();}}
}
