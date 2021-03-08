package com.telefonica

object Main {
  def main(args: Array[String]): Unit = {
    println("Testing SftpClient")
    
    val ppal = new SftpClient()
    val sftp = ppal.setupConnection("gabi", "123", "127.0.0.1", "22")
//    ppal.listarArquivos(sftp, "/home/gabi/ftp/")
    println("--------------")
    ppal.downloadDesdeSftp(sftp, "/home/hdp/Documents/directoriosftp/", "/home/gabi/ftp/")
    
  }
}