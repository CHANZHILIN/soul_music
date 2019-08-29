package com.soul_music.utils

import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @author cokus
 * 将pcm转换成wav格式
 * 其实就是加入wav头
 */
class Pcm2Wav {

    @Throws(Exception::class)
    fun convertAudioFiles(src: String, target: String) {
        var fis = FileInputStream(src)
        val fos = FileOutputStream(target)


        val buf = ByteArray(1024 * 1000)
        var size = fis.read(buf)
        var PCMSize = 0
        while (size != -1) {
            PCMSize += size
            size = fis.read(buf)
        }
        fis.close()


        val header = WaveHeader()
        header.fileLength = PCMSize + (44 - 8)
        header.FmtHdrLeth = 16
        header.BitsPerSample = 16
        header.Channels = 1
        header.FormatTag = 0x0001
        header.SamplesPerSec = 16000
        header.BlockAlign = (header.Channels * header.BitsPerSample / 8).toShort()
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec
        header.DataHdrLeth = PCMSize

        val h = header.header

        assert(h.size == 44)
        //write header
        fos.write(h, 0, h.size)
        //write data stream
        fis = FileInputStream(src)
        size = fis.read(buf)
        while (size != -1) {
            fos.write(buf, 0, size)
            size = fis.read(buf)
        }
        fis.close()
        fos.close()
    }
}