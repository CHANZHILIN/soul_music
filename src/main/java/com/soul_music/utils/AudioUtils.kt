package com.soul_music.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object AudioUtils {
    //	private int size;


    fun hebing(paths: List<String>, savaPath: String): Boolean {
        try {
            for (i in paths.indices) {
                val fos = FileOutputStream(savaPath + "bbbb.wav")
                val fis = FileInputStream(paths[i])
                val temp = ByteArray(fis.available())
                val len = temp.size
                if (i == 0) {
                    while (fis.read(temp) > 0) {
                        fos.write(temp, 0, len)
                    }
                } else {
                    while (fis.read(temp) > 0) {
                        fos.write(temp, 44, len - 44)
                    }
                }
                fos.flush()
                fis.close()
            }

        } catch (e: IOException) {
            return false
        }

        return true

    }

    /**
     * @param partsPaths     要合成的音频路径数组
     * @param unitedFilePath 输入合并结果数组
     */
    fun uniteWavFile(partsPaths: List<String>, unitedFilePath: String): Boolean {


        for (i in partsPaths.indices) {
            val f = File(partsPaths[i])
            try {
                val `in` = FileInputStream(f)
                val bytes = ByteArray(44)
                `in`.read(bytes)

                for (j in bytes.indices) {
                    println("--------->" + bytes[i])
                }

            } catch (e: FileNotFoundException) {
                return false
            } catch (e: IOException) {
                return false
            }

        }

        return true
    }

    private fun getByte(path: String): ByteArray? {
        val f = File(path)
        val `in`: InputStream
        var bytes: ByteArray? = null
        try {
            `in` = FileInputStream(f)
            bytes = ByteArray(f.length().toInt())
            `in`.read(bytes)
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bytes
    }


    /**
     * merge *.wav files
     *
     * @param target output file
     * @param paths  the files that need to merge
     * @return whether merge files success
     */
    fun mergeAudioFiles(target: String, paths: List<String>): Boolean {
        try {
            val fos = FileOutputStream(target)
            var size = 0
            val buf = ByteArray(1024 * 1000)
            var PCMSize = 0
            for (i in paths.indices) {
                val fis = FileInputStream(paths[i])
                size = fis.read(buf)
                while (size != -1) {
                    PCMSize += size
                    size = fis.read(buf)
                }
                fis.close()
            }
            PCMSize = PCMSize - paths.size * 44
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
            fos.write(h, 0, h.size)
            for (j in paths.indices) {
                val fis = FileInputStream(paths[j])
                size = fis.read(buf)
                var isFirst = true
                while (size != -1) {
                    if (isFirst) {
                        fos.write(buf, 44, size - 44)
                        size = fis.read(buf)
                        isFirst = false
                    } else {
                        fos.write(buf, 0, size)
                        size = fis.read(buf)
                    }
                }
                fis.close()
            }
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }


    /**
     * 文件的部分剪辑（单端删除类型）
     *
     * @param target    目标输出文件
     * @param dest      源文件（wav）
     * @param cutPos  开始开始剪贴点
     * @param endPos    剪贴结束点
     * @param pcmTarget 源pcm文件
     * @return
     */
    fun cutAudioFiles(target: String, dest: String, cutPos: Int, endPos: Int, pcmTarget: String, pamOut: String): Boolean {
        try {

            val file = File(pcmTarget)
            val fileSize = file.length()
            val fos = FileOutputStream(target)
            val fos1 = FileOutputStream(pamOut)

            val size = 0
            var PCMSize = 0
            PCMSize = (fileSize - (endPos - cutPos)).toInt()

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


            fos.write(h, 0, h.size)
            val fis = FileInputStream(dest)
            val fis1 = FileInputStream(file)


            val buf = ByteArray(cutPos)
            var buf2: ByteArray? = ByteArray(cutPos)

            //读前半部分
            fis.read(buf)
            fis1.read(buf2)


            //写前半部分
            fos1.write(buf2)
            if (buf.size > 44) {
                fos.write(buf, 44, buf.size - 44)
            }
            //计算第二部分的长度
            var buf1: ByteArray? = ByteArray((fileSize - endPos).toInt())
            var buf3: ByteArray? = ByteArray((fileSize - endPos).toInt())


            //如果不是末尾点，则进行跳读
            if (buf1!!.size != 0) {
                //wav 文件格式的读取
                fis.skip(endPos.toLong())//流字节跳转到末尾点
                fis.read(buf1)//末尾点到文件的总长度
                fos.write(buf1, 0, buf1.size)


                //pcm 文件的格式的读取
                fis1.skip(endPos.toLong())
                fis1.read(buf3)
                fos1.write(buf3)
            }
            fis.close()
            fos.close()
            fos1.close()
            buf1 = null
            buf2 = null
            buf3 = null
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }


    /**
     * 音频的多段操作（pcm格式音频）
     *
     * @param dstPath 源文件
     * @param outPath 输出文件
     * @param cutAreas 裁剪编辑的区域
     */
    fun getPcmEdits(dstPath: String, outPath: String, cutAreas: List<LongArray>?) {

        val dstFile = File(dstPath)
        val outFile = File(outPath)
        val dstFileLength = dstFile.length().toInt()
        if (dstFile.exists() && cutAreas != null) {
            try {
                val `in` = FileInputStream(dstFile)
                val out = FileOutputStream(outFile)
                //	    		for(int i=0;i<cutAreas.size();i++){
                //	    			int[] cutArea=cutAreas.get(i);
                //	    			totalSize=totalSize+(cutArea[1]-cutArea[0]);
                //	    		}
                //	    		WaveHeader header = new WaveHeader();
                //		        header.fileLength = totalSize + (44 - 8);
                //		        header.FmtHdrLeth = 16;
                //		        header.BitsPerSample = 16;
                //		        header.Channels = 1;
                //		        header.FormatTag = 0x0001;
                //		        header.SamplesPerSec = 16000;
                //		        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
                //		        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
                //		        header.DataHdrLeth = totalSize;
                //		        byte[] h = header.getHeader();
                //		        assert h.length == 44;
                //		        out.write(h, 0, h.length);
                var index = 0
                while (index < cutAreas.size) {
                    if (index == 0) {
                        if (cutAreas.size > 1) {
                            if (cutAreas[index][0] != 0L) {
                                val buf = ByteArray(cutAreas[index][0].toInt())
                                `in`.read(buf)
                                out.write(buf)
                            }

                        } else {
                            if (cutAreas[index][0] != 0L) {
                                val buf = ByteArray(cutAreas[index][0].toInt())
                                `in`.read(buf)
                                out.write(buf)
                            }

                            if (dstFileLength - cutAreas[index][1].toInt() != 0) {
                                val buf1 = ByteArray(dstFileLength - cutAreas[index][1].toInt())
                                `in`.skip(cutAreas[index][1])
                                `in`.read(buf1)
                                out.write(buf1)
                            }

                        }
                    } else {
                        val buf = ByteArray((cutAreas[index][0] - cutAreas[index - 1][1]).toInt())
                        `in`.skip(cutAreas[index - 1][1])
                        `in`.read(buf)
                        out.write(buf)
                        if (cutAreas[index][1] < dstFileLength) {
                            val buf1 = ByteArray((dstFileLength - cutAreas[index][1]).toInt())
                            `in`.skip(cutAreas[index][1])
                            `in`.read(buf1)
                            out.write(buf1)
                        }
                    }
                    index = index + 1
                }
                //读写完毕
                `in`.close()
                out.close()
                dstFile.delete()
                //		        outFile.renameTo(dstFile);
                pcm2wav(outFile.absolutePath, outFile.absolutePath.replace(".pcm", ".wav"))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }


    /**
     * pcm转换成wav
     *
     * @param pcmPath
     * @param wavPath
     */
    fun pcm2wav(pcmPath: String, wavPath: String) {
        val p2w = Pcm2Wav()
        try {
            p2w.convertAudioFiles(pcmPath, wavPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    /**
     * 根据当前的像素点位置，获取对应的当前数据源的位置（有误差）
     *
     * @param totlePixs   waveView控件的总长度
     * @param currentPixs 当前像素点
     * @param pcmSize     pcm数据的总长
     * @return
     */
    fun getCurrentPos(totlePixs: Int, currentPixs: Int, pcmSize: Long): Long {
        var result: Long = 0
        if (totlePixs != 0) {
            result = pcmSize * currentPixs / totlePixs
        }
        return result


    }


}
