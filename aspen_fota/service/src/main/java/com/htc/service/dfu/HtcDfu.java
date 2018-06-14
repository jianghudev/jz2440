package com.htc.service.dfu;

import android.util.Log;

import com.htc.service.Const;
import com.htc.service.usb.Usb;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Created by wanjin_shi on 16-10-20.
 */
public class HtcDfu {
    private static final String TAG= Const.G_TAG;

    private final static int USB_DIR_OUT = 0;
    private final static int USB_DIR_IN = 128;
    private final static int DFU_RequestType = 0x21;

    //DFU Requests DFU states
    private final static int HTC_APP_STATE_IDLE = 0x00;
    private final static int HTC_APP_STATE_DETACH = 0x01;
    private final static int HTC_DFU_STATE_IDLE = 0x02;
    private final static int HTC_DFU_STATE_DOWNLOAD_SYNC = 0x03;
    private final static int HTC_DFU_STATE_DOWNLOAD_BUSY = 0x04;
    private final static int HTC_DFU_STATE_DOWNLOAD_IDLE = 0x05;
    private final static int HTC_DFU_STATE_MANIFEST_SYNC = 0x06;
    private final static int HTC_DFU_STATE_MANIFEST = 0x07;
    private final static int HTC_DFU_STATE_MANIFEST_WAIT_RESET = 0x08;
    private final static int HTC_DFU_STATE_UPLOAD_IDLE = 0x09;
    private final static int HTC_DFU_STATE_ERROR = 0x0A;
    private final static int HTC_DFU_STATE_MOVEING_DONE = 0x0B;

    // DFU class requests code
    private final static int DFU_DETACH = 0x00;
    private final static int DFU_DNLOAD = 0x01;
    private final static int DFU_UPLOAD = 0x02;
    private final static int DFU_GETSTATUS = 0x03;
    private final static int DFU_CLRSTATUS = 0x04;
    private final static int DFU_GETSTATE = 0x05;
    private final static int DFU_ABORT = 0x06;
    private final static int DFU_MOVEIMG = 0x07;
    Usb m_Usb;
    int m_DeviceVID;
    int m_DevicePID;
    HtcDfuFile m_HtcDfuFile;

    public HtcDfu(int usbVId, int usbPId) {
        m_DeviceVID = usbVId;
        m_DevicePID = usbPId;
        m_HtcDfuFile = new HtcDfuFile();
    }

    public void setUsb(Usb usb) {
        m_Usb = usb;
    }

    public boolean EraseFotaSector(int address) {
        if (m_Usb == null || !m_Usb.UsbIsConnected()) {
            Log.i(TAG,"usb is null.");
            return false;
        }
        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        DFU_Request dfu_request = new DFU_Request();
        try {
            int retry_count =6;
            while (retry_count -- > 0){
                while (dfuStatus.bState != HTC_DFU_STATE_IDLE){
                    HTCDFU_ClearStatus();
                    HTCDFU_GetStatus(dfuStatus);
                }
                if (isAddressProtected(address)) {
                    Log.i(TAG,"Device fota partition is read protected.");
                    unProtectCommand();
                    continue;
                }
                dfu_request.data = new byte[5];
                dfu_request.data[0] = 0x41;
                dfu_request.data[1] = (byte) (address & 0xFF);
                dfu_request.data[2] = (byte) ((address >> 8) & 0xFF);
                dfu_request.data[3] = (byte) ((address >> 16) & 0xFF);
                dfu_request.data[4] = (byte) ((address >> 24) & 0xFF);
                dfu_request.length = 5;
                dfu_request.DfuOperation = DFU_DNLOAD;
                dfu_request.block = 0;
                DFU_LaunchOperation(dfu_request);

                int download_retry=0;
                HTCDFU_GetStatus(dfuStatus);
                while (dfuStatus.bState != HTC_DFU_STATE_DOWNLOAD_IDLE ){
                    if(download_retry++>=2000){   //// retry 10 secound
                        break;
                    }
                    Thread.sleep(2);
                    Log.i(TAG,"1 dfu status="+ dfuStatus.bState+" retry="+download_retry);
                    HTCDFU_GetStatus(dfuStatus);
                }
                dfu_request.DfuOperation = DFU_ABORT;
                DFU_LaunchOperation(dfu_request);
                HTCDFU_GetStatus(dfuStatus);
                Log.i(TAG,"2 dfu status="+ dfuStatus.bState);
                if(dfuStatus.bState != HTC_DFU_STATE_IDLE){
                    Log.i(TAG,"err! dfu status="+ dfuStatus.bState+" continue retry!");
                    continue;
                }

                Log.i(TAG,"Erase sector addr=0x"+ Integer.toHexString(address) +" done");
                return true;
            } ;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean UpgradeFotaImage(File file) {
        int address = 0;
        int BufferOffset = 0;
        int blockSize = 1024;
        byte[] Block = new byte[blockSize];
        int NumOfBlocks = 0;
        int blockNum;
        boolean UpgradeFotaImageResult = false;
        boolean analysysdfufileresult = false;
        long startWriteTime = 0;
        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        DFU_Request dfu_request = new DFU_Request();

        if (m_Usb == null || !m_Usb.UsbIsConnected()) {
            Log.i(TAG,"No device connected");
            UpgradeFotaImageResult = false;
        }
        try {
            if (isAddressProtected(m_HtcDfuFile.FirmWareStartAddress)) {
                Log.i(TAG,"Device fota partition is read protected.");
                unProtectCommand();
                return false;
            }
            analysysdfufileresult = m_HtcDfuFile.AnalysisDfuFile(file);
            if(analysysdfufileresult) {
                Log.i(TAG,"AnalysisDfuFile finish");
                address = m_HtcDfuFile.FirmWareStartAddress;
                BufferOffset = m_HtcDfuFile.FirmWareOffset;
                blockSize = m_HtcDfuFile.maxBlockSize;
                Block = new byte[blockSize];
                NumOfBlocks = m_HtcDfuFile.FirmWareLength / blockSize;
                if(address != m_HtcDfuFile.FirmWareStartAddress){
                    Log.i(TAG, "dfu file startaddress is error, it must be 0x08010000");
                    return false;
                }

                if ((m_DevicePID != m_HtcDfuFile.getpid()) || (m_DeviceVID != m_HtcDfuFile.getvid())) {
                    Log.i(TAG, "PID/VID match error.");
                    return false;
                }


                startWriteTime = System.currentTimeMillis();
                Log.i(TAG,"Download fota image start time:" + startWriteTime + " ms\n");
                for (blockNum = 0; blockNum < NumOfBlocks; blockNum++) {
//                    Log.i(TAG,"blockNum:" + blockNum);
                    System.arraycopy(m_HtcDfuFile.m_DfuFilebuffer, (blockNum * blockSize) + BufferOffset, Block, 0, blockSize);
                    if (blockNum == 0) {
                        setAddressPointer(address);
                        HTCDFU_GetStatus(dfuStatus);
                        HTCDFU_GetStatus(dfuStatus);
                        if (dfuStatus.bState == HTC_DFU_STATE_ERROR) {
                            Log.i(TAG, "set address fail");
                            UpgradeFotaImageResult = false;
                            break;
                        }
                    }
//                    Log.i(TAG,"dfuStatus.bState" + dfuStatus.bState);
                    while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
//                        Log.i(TAG,"dfuStatus.bState" + dfuStatus.bState);
                        HTCDFU_ClearStatus();
                        HTCDFU_GetStatus(dfuStatus);
                    }
                    dfu_request.DfuOperation = DFU_DNLOAD;
                    dfu_request.data = new byte[Block.length];
                    dfu_request.data = Block;
                    dfu_request.length = Block.length;
                    dfu_request.block = blockNum + 2;
                    DFU_LaunchOperation(dfu_request);
                    HTCDFU_GetStatus(dfuStatus);
                    HTCDFU_GetStatus(dfuStatus);
                    if (dfuStatus.bState == HTC_DFU_STATE_ERROR) {
                        Log.i(TAG, "write block data fail");
                        UpgradeFotaImageResult = false;
                        break;
                    }
                    while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
                        HTCDFU_ClearStatus();
                        HTCDFU_GetStatus(dfuStatus);
                    }
                }
                int remainder = m_HtcDfuFile.FirmWareLength - (blockNum * blockSize);
                if (remainder > 0) {
                    System.arraycopy(m_HtcDfuFile.m_DfuFilebuffer, (blockNum * blockSize) + BufferOffset, Block, 0, remainder);
                    while (remainder < Block.length) {
                        Block[remainder++] = (byte) 0xFF;
                    }
                    while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
                        HTCDFU_ClearStatus();
                        HTCDFU_GetStatus(dfuStatus);
                    }
                    dfu_request.DfuOperation = DFU_DNLOAD;
                    dfu_request.data = new byte[Block.length];
                    dfu_request.data = Block;
                    dfu_request.length = Block.length;
                    dfu_request.block = blockNum + 2;
                    DFU_LaunchOperation(dfu_request);
                    HTCDFU_GetStatus(dfuStatus);
                    HTCDFU_GetStatus(dfuStatus);
                    if (dfuStatus.bState == HTC_DFU_STATE_ERROR) {
                        Log.i(TAG, "write block data fail");
                        UpgradeFotaImageResult = false;
                    }
                    while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
                        HTCDFU_ClearStatus();
                        HTCDFU_GetStatus(dfuStatus);
                    }

                }
            }
            Log.i(TAG,"Download fota image completed time: " + (System.currentTimeMillis() - startWriteTime) + " ms\n");
            UpgradeFotaImageResult = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,e.toString());
        }
        return UpgradeFotaImageResult;
    }

    public boolean moveImage()
    {
        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        DFU_Request dfu_request = new DFU_Request();
        int count = 0;
        try {
            dfu_request.DfuOperation = DFU_MOVEIMG;
            DFU_LaunchOperation(dfu_request);
            do {
                Thread.sleep(1000);
                HTCDFU_GetStatus(dfuStatus);
                Log.i(TAG,"while bState = " + dfuStatus.bState);
                count ++;
                if(count == 50)
                    break;
            } while (dfuStatus.bState != HTC_DFU_STATE_MOVEING_DONE);
            Log.i(TAG,"Move image return ,bStatus = " + dfuStatus.bStatus + "count = " + count);
            if(dfuStatus.bStatus == 0&&count < 50) {
                Log.i(TAG,"Move image true");
                return true;//move successful
            }else {
                return false;//move fail
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    public boolean LaunchVerify(File file) {
        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        DFU_Request dfu_request = new DFU_Request();
        int fwaddress = 0;
        int blockSize = 1024;
        byte[] Block = new byte[blockSize];
        int NumOfBlocks = 0;
        int fwlenth = 0;
        int blockNum;
        boolean LaunchVerifyResult = true;
        boolean analysysdfufileresult = false;

        if (m_Usb == null || !m_Usb.UsbIsConnected()) {
            Log.i(TAG,"No device connected");
            return false;
        }

        try {
            if (isAddressProtected(m_HtcDfuFile.FirmWareStartAddress)) {
                Log.i(TAG,"Device fota partition is read protected.");
                unProtectCommand();
                return false;
            }

            if (m_HtcDfuFile.m_DfuFilePath == null) {
                analysysdfufileresult = m_HtcDfuFile.AnalysisDfuFile(file);
                if ((m_DevicePID != m_HtcDfuFile.getpid()) || (m_DeviceVID != m_HtcDfuFile.getvid())) {
                    Log.i(TAG,"PID/VID Miss match");
                    return false;
                }
            }
            fwaddress = m_HtcDfuFile.FirmWareStartAddress;
            blockSize = m_HtcDfuFile.maxBlockSize;
            Block = new byte[blockSize];
            NumOfBlocks = m_HtcDfuFile.FirmWareLength / blockSize;
            fwlenth = m_HtcDfuFile.FirmWareLength;


            byte[] deviceFirmware = new byte[m_HtcDfuFile.FirmWareLength];
            //readImage(deviceFirmware);
            while (dfuStatus.bState != HTC_DFU_STATE_IDLE){
                HTCDFU_ClearStatus();
                HTCDFU_GetStatus(dfuStatus);
            }

            setAddressPointer(fwaddress);
            HTCDFU_GetStatus(dfuStatus);
            HTCDFU_GetStatus(dfuStatus);
            if (dfuStatus.bState == HTC_DFU_STATE_ERROR) {
                Log.i(TAG,"setAddressPointer fail.");
                return false;
            }
            for (blockNum = 0; blockNum <= NumOfBlocks; blockNum++) {

                while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
                    HTCDFU_ClearStatus();
                    HTCDFU_GetStatus(dfuStatus);
                }
                dfu_request.DfuOperation = DFU_UPLOAD;
                dfu_request.data = new byte[Block.length];
                dfu_request.data = Block;
                dfu_request.length = Block.length;
                dfu_request.block = blockNum+2;
                DFU_LaunchOperation(dfu_request);
                HTCDFU_GetStatus(dfuStatus);

                if (fwlenth >= blockSize) {
                    fwlenth -= blockSize;
                    System.arraycopy(Block, 0, deviceFirmware, (blockNum * blockSize), blockSize);
                } else {
                    System.arraycopy(Block, 0, deviceFirmware, (blockNum * blockSize), fwlenth);
                }
            }

            ByteBuffer dfuFileBuffer = ByteBuffer.wrap(m_HtcDfuFile.m_DfuFilebuffer, m_HtcDfuFile.FirmWareOffset, m_HtcDfuFile.FirmWareLength);
            ByteBuffer deviceImageBuffer = ByteBuffer.wrap(deviceFirmware);

            if (dfuFileBuffer.equals(deviceImageBuffer) ) {
                Log.i(TAG,"device firmware equals file firmware");
                return true;
            } else {
                Log.i(TAG,"device firmware does not equals file firmware");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,e.toString());
        }
        return true;
    }

    public void leaveDfuMode(int Address) throws Exception {

        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        DFU_Request dfu_request = new DFU_Request();
        HTCDFU_GetStatus(dfuStatus);
        while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
            HTCDFU_ClearStatus();
            HTCDFU_GetStatus(dfuStatus);
        }
        // set restart system start address.
        setAddressPointer(Address);
        HTCDFU_GetStatus(dfuStatus);
        while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
            HTCDFU_ClearStatus();
            HTCDFU_GetStatus(dfuStatus);
        }
        dfu_request.DfuOperation = DFU_DNLOAD;
        dfu_request.data = null;
        dfu_request.length = 0;
        DFU_LaunchOperation(dfu_request);
        HTCDFU_GetStatus(dfuStatus);
        HTCDFU_GetStatus(dfuStatus);
        while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
            HTCDFU_ClearStatus();
            HTCDFU_GetStatus(dfuStatus);
        }
    }

    private boolean isAddressProtected(int address) throws Exception {
        Log.i(TAG,"enter isAddressProtected");
        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        boolean ReadProtected = false;

        HTCDFU_GetStatus(dfuStatus);
        while (dfuStatus.bState != HTC_DFU_STATE_IDLE) {
            HTCDFU_ClearStatus();
            HTCDFU_GetStatus(dfuStatus);
        }

        setAddressPointer(address);
        HTCDFU_GetStatus(dfuStatus);
        HTCDFU_GetStatus(dfuStatus);

        if (dfuStatus.bState == HTC_DFU_STATE_ERROR) {
            ReadProtected = true;
        }

        while (dfuStatus.bState != HTC_DFU_STATE_IDLE){
            HTCDFU_ClearStatus();
            HTCDFU_GetStatus(dfuStatus);
        }
        Log.i(TAG,"isAddressProtecteddfuStatus.bState=" + dfuStatus.bState+" protected="+ReadProtected);
        return ReadProtected;
    }

    private void setAddressPointer(int Address) throws Exception {
        Log.i(TAG,"enter setAddressPointer");
        DFU_Request dfu_request = new DFU_Request();
        dfu_request.DfuOperation = DFU_DNLOAD;
        dfu_request.data = new byte[5];
        dfu_request.data[0]= 0x21;
        dfu_request.data[1] = (byte) (Address & 0xFF);
        dfu_request.data[2] = (byte) ((Address >> 8) & 0xFF);
        dfu_request.data[3] = (byte) ((Address >> 16) & 0xFF);
        dfu_request.data[4] = (byte) ((Address >> 24) & 0xFF);
        dfu_request.length = 5;
        dfu_request.block = 0;
        DFU_LaunchOperation(dfu_request);
    }

    private void unProtectCommand() throws Exception {
        Log.i(TAG,"enter unProtectCommand");
        HtcDfuStatus dfuStatus = new HtcDfuStatus();
        DFU_Request dfu_request = new DFU_Request();
        dfu_request.DfuOperation = DFU_DNLOAD;
        dfu_request.data = new byte[1];
        dfu_request.data[0] = (byte) 0x92;
        dfu_request.length = 1;
        dfu_request.block = 0;
        DFU_LaunchOperation(dfu_request);
        HTCDFU_GetStatus(dfuStatus);
        if (dfuStatus.bState != HTC_DFU_STATE_DOWNLOAD_BUSY) {
            throw new Exception("Failed to execute unprotect command");
        }
    }

    private void DFU_LaunchOperation(DFU_Request dfu_request) throws Exception{
        boolean value = true;
        UsbRequest usb_request = new UsbRequest();
//        Log.i(TAG,"enter DFU_LaunchOperation");
        switch (dfu_request.DfuOperation){
            case DFU_DNLOAD: {
//                Log.i(TAG,"DFU_DNLOAD");
                if(dfu_request.block > 0) {
                    usb_request.RequestType = 0x21;
                    usb_request.Request = DFU_DNLOAD;
                    usb_request.Value = dfu_request.block;
                    usb_request.Index = 0;
                    usb_request.buffer = dfu_request.data;
                    usb_request.Length = dfu_request.length;
                    usb_request.timeout = 0;
                }else{
                    usb_request.RequestType = 0x21;
                    usb_request.Request = DFU_DNLOAD;
                    usb_request.Value = 0;
                    usb_request.Index = 0;
                    usb_request.buffer = dfu_request.data;
                    usb_request.Length = dfu_request.length;
                    usb_request.timeout = 0;
                }
                break;
            }
            case DFU_UPLOAD: {
//                Log.i(TAG,"DFU_UPLOAD");
                usb_request.RequestType = 0x21 | 0x80;
                usb_request.Request = DFU_UPLOAD;
                usb_request.Value = dfu_request.block;
                usb_request.Index = 0;
                usb_request.buffer = dfu_request.data;
                usb_request.Length = dfu_request.length;
                usb_request.timeout = 100;
                break;
            }
            case DFU_ABORT: {
//                Log.i(TAG,"DFU_ABORT");
                usb_request.RequestType = 0x21;
                usb_request.Request = DFU_ABORT;
                usb_request.Value = 0;
                usb_request.Index = 0;
                usb_request.buffer = null;
                usb_request.Length = 0;
                usb_request.timeout = 0;
                break;
            }
            case DFU_MOVEIMG: {
//                Log.i(TAG,"DFU_MOVEIMG");
                usb_request.RequestType = 0x21;
                usb_request.Request = DFU_MOVEIMG;
                usb_request.Value = 0;
                usb_request.Index = 0;
                usb_request.buffer = null;
                usb_request.Length = 0;
                usb_request.timeout = 0;
                break;
            }
            default:
                value = false;
                break;
        }
        if(value) {
            int length = m_Usb.UsbControlTransfer(usb_request.RequestType, usb_request.Request, usb_request.Value, usb_request.Index, usb_request.buffer, usb_request.Length, usb_request.timeout);

            if (length < 0) {
                throw new Exception("USB failed during DFU_LaunchOperation " + dfu_request.DfuOperation);
            }
        } else {
            Log.i(TAG,"dfu request operation is error.");
        }

    }

    private void HTCDFU_GetStatus(HtcDfuStatus status) {
        //Log.i(TAG,"enter HTCDFU_GetStatus");
        UsbRequest usb_request = new UsbRequest();
        usb_request.RequestType = 0x21 | 0x80;
        usb_request.Request = DFU_GETSTATUS;
        usb_request.Value = 0;
        usb_request.Index = 0;
        usb_request.buffer = new byte[6];
        usb_request.Length = 6;
        usb_request.timeout = 500;
        if(m_Usb == null){
            Log.i(TAG,"m_Usb is null when dfu getstatus");
        }else {
            int length = m_Usb.UsbControlTransfer(usb_request.RequestType, usb_request.Request, usb_request.Value, usb_request.Index, usb_request.buffer, usb_request.Length, usb_request.timeout);
            if (length < 0) {
                Log.i(TAG,"HTCDFU_GetStatus fail.");
            } else {
                status.bStatus = usb_request.buffer[0];
                status.bState = usb_request.buffer[4];
                status.bwPollTimeout = (usb_request.buffer[3] & 0xFF) << 16;
                status.bwPollTimeout |= (usb_request.buffer[2] & 0xFF) << 8;
                status.bwPollTimeout |= (usb_request.buffer[1] & 0xFF);
            }
        }
    }

    private void HTCDFU_ClearStatus() {
        //Log.i(TAG,"enter HTCDFU_ClearStatus");
        UsbRequest usb_request = new UsbRequest();
        usb_request.RequestType = 0x21;
        usb_request.Request = DFU_CLRSTATUS;
        usb_request.Value = 0;
        usb_request.Index = 0;
        usb_request.buffer = null;
        usb_request.Length = 0;
        usb_request.timeout = 0;
        if(m_Usb == null){
            Log.i(TAG,"get status m_Usb is null");
        }else {
            int length = m_Usb.UsbControlTransfer(usb_request.RequestType, usb_request.Request, usb_request.Value, usb_request.Index, usb_request.buffer, usb_request.Length, usb_request.timeout);
            if (length < 0) {
                Log.i(TAG,"usb ControlTransfer fail when dfu clearstatus.");
            }
        }
    }

    class UsbRequest{
        int RequestType;
        int Request;
        int Value;
        int Index;
        byte[] buffer;
        int Length;
        int timeout;
    }

    class DFU_Request {
        byte[] data;
        int length;
        int block;
        int DfuOperation;
    }


    private class HtcDfuStatus {
        byte bStatus;
        byte bState;
        int bwPollTimeout;
    }

}
