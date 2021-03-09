package com.telefonica

import org.apache.log4j.Logger

object Main {
  def main(args: Array[String]): Unit = {
    @transient
    lazy val log: Logger = Logger.getLogger(getClass.getName)
  
    println("Testing SftpClient")
    
    val username = args(0)
    val password = args(1)
    val host = args(2)
    val port = args(3)
    val localPath = args(4)
    val remotePath = args(5)
    
    val ppal = new SftpClient()
    val sftp = ppal.setupConnection(username, password, host, port)
    
    if(args(6) == "LST"){
      ppal.listarArquivos(sftp, remotePath)
    } else if(args(6) == "DWN"){
      ppal.downloadDesdeSftp(sftp, localPath, remotePath)
    } else if(args(6) == "UPL"){
      ppal.uploadDesdeLocal(sftp, localPath, remotePath)
    }
  }
}