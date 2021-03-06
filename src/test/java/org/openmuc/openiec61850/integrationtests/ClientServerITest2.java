package org.openmuc.openiec61850.integrationtests;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.openiec61850.BasicDataAttribute;
import org.openmuc.openiec61850.BdaReasonForInclusion;
import org.openmuc.openiec61850.ClientAssociation;
import org.openmuc.openiec61850.ClientEventListener;
import org.openmuc.openiec61850.ClientSap;
import org.openmuc.openiec61850.Fc;
import org.openmuc.openiec61850.FcModelNode;
import org.openmuc.openiec61850.ModelNode;
import org.openmuc.openiec61850.Report;
import org.openmuc.openiec61850.SclParseException;
import org.openmuc.openiec61850.SclParser;
import org.openmuc.openiec61850.ServerEventListener;
import org.openmuc.openiec61850.ServerModel;
import org.openmuc.openiec61850.ServerSap;
import org.openmuc.openiec61850.ServiceError;

public class ClientServerITest2 extends Thread implements ServerEventListener, ClientEventListener {

    int port = 54322;
    String host = "127.0.0.1";
    ClientSap clientSap = new ClientSap();
    ServerSap serverSap = null;
    ClientAssociation clientAssociation = null;
    ClientAssociation clientAssociation2 = null;
    ServerModel serversServerModel = null;
    private static int numReports = 0;
    private static int numSuccess = 0;
    private static int numAssociationClosed = 0;

    // Get the Java runtime
    public static Runtime runtime = Runtime.getRuntime();

    @Test
    public void testClientServerCom() throws IOException, ServiceError, ConfigurationException,
            javax.naming.ConfigurationException, SclParseException, InterruptedException {

        clientSap.setTSelRemote(new byte[] { 0, 1 });
        clientSap.setTSelLocal(new byte[] { 0, 0 });
        clientSap.setApTitleCalled(new int[] { 1, 1, 999, 1 });

        runServer("src/test/resources/testModel2.icd", port);
        System.out.println("IED Server is running");

        // -----------------------------------------------------------
        // Client
        // -----------------------------------------------------------

        System.out.println("Attempting to connect to server " + host + " on port " + port);

        clientAssociation = clientSap.associate(InetAddress.getByName(host), port, null, this);

        ServerModel serverModel = SclParser.parse("src/test/resources/testModel2.icd").get(0);
        clientAssociation.setServerModel(serverModel);

        getAllBdas(serverModel);

        // timestamp = (BdaTimestamp) serverModel.findModelNode("ied1lDevice1/MMXU1.TotW.t", Fc.MX);
        // clientAssociation.getDataValues(timestamp);

        clientAssociation.disconnect();

        Thread.sleep(500);
        serverSap.stop();

    }

    private void getAllBdas(ServerModel serverModel) throws ServiceError, IOException {

        for (ModelNode ld : serverModel) {
            for (ModelNode ln : ld) {
                getDataRecursive(ln, clientAssociation);
            }
        }
    }

    private void runServer(String sclFilePath, int port) throws SclParseException, IOException {

        serverSap = new ServerSap(port, 0, null, SclParser.parse(sclFilePath).get(0), null);

        serverSap.setPort(port);
        serverSap.startListening(this);
        serversServerModel = serverSap.getModelCopy();
        start();
    }

    private static void getDataRecursive(ModelNode modelNode, ClientAssociation clientAssociation)
            throws ServiceError, IOException {
        if (modelNode.getChildren() == null) {
            return;
        }
        for (ModelNode childNode : modelNode) {
            FcModelNode fcChildNode = (FcModelNode) childNode;
            if (fcChildNode.getFc() != Fc.CO) {
                System.out.println("calling GetDataValues(" + childNode.getReference() + ")");
                clientAssociation.getDataValues(fcChildNode);
            }
            // clientAssociation.setDataValues(fcChildNode);
            getDataRecursive(childNode, clientAssociation);
        }
    }

    public static int findArray(Byte[] array, Byte[] subArray) {
        return Collections.indexOfSubList(Arrays.asList(array), Arrays.asList(subArray));
    }

    public static String getByteArrayString(byte[] byteArray) {
        StringBuilder builder = new StringBuilder();
        int l = 1;
        for (byte b : byteArray) {
            if ((l != 1) && ((l - 1) % 8 == 0)) {
                builder.append(' ');
            }
            if ((l != 1) && ((l - 1) % 16 == 0)) {
                builder.append('\n');
            }
            l++;
            builder.append("0x");
            String hexString = Integer.toHexString(b & 0xff);
            if (hexString.length() == 1) {
                builder.append(0);
            }
            builder.append(hexString + " ");
        }
        return builder.toString();
    }

    @Override
    public void serverStoppedListening(ServerSap serverSAP) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<ServiceError> write(List<BasicDataAttribute> bdas) {
        System.out.println("DataSource: got write request");
        return null;
    }

    @Override
    public void run() {

        // BdaFloat32 totWMag = (BdaFloat32) serversServerModel.findModelNode("ied1lDevice1/MMXU1.TotW.mag.f", Fc.MX);
        // BdaQuality q = (BdaQuality) serversServerModel.findModelNode("ied1lDevice1/MMXU1.TotW.q", Fc.MX);
        // BdaTimestamp t = (BdaTimestamp) serversServerModel.findModelNode("ied1lDevice1/MMXU1.TotW.t", Fc.MX);
        //
        // List<BasicDataAttribute> totWBdas = new ArrayList<BasicDataAttribute>(3);
        // totWBdas.add(totWMag);
        // totWBdas.add(q);
        // totWBdas.add(t);
        //
        // float totWMagVal = 0.0f;
        // q.setValidity(BdaQuality.Validity.GOOD);
        //
        // // for (int i = 0; i < 500000; i++) {
        //
        // totWMagVal += 1.0;
        //
        // logger.info("setting totWmag to: " + totWMagVal);
        // totWMag.setFloat(totWMagVal);
        // t.setCurrentTime();
        //
        // if (q.getValidity() == Validity.GOOD) {
        // q.setValidity(BdaQuality.Validity.INVALID);
        // }
        // else {
        // q.setValidity(BdaQuality.Validity.GOOD);
        // }
        //
        // try {
        // Thread.sleep(4000);
        // } catch (InterruptedException e) {
        // }
        // serverSap.setValues(totWBdas);

        // // Run the garbage collector
        // runtime.gc();
        // // Calculate the used memory
        // long memory = runtime.totalMemory() - runtime.freeMemory();
        // System.out.println("Used memory is bytes: " + memory);
        // System.out.println("Used memory is megabytes: " + bytesToMegabytes(memory));

        // try {
        // Thread.sleep(2000);
        // } catch (InterruptedException e) {
        // }
        // }

    }

    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    @Override
    public void newReport(Report report) {
        System.out.println("got report!");
        numReports++;

        if (numReports == 1) {
            List<BdaReasonForInclusion> reasons = report.getReasonCodes();
            Assert.assertEquals(2, reasons.size());
            Assert.assertTrue(reasons.get(0).isGeneralInterrogation());
            Assert.assertFalse(reasons.get(0).isDataUpdate());
        }
        else if (numReports == 2) {
            List<BdaReasonForInclusion> reasons = report.getReasonCodes();
            Assert.assertEquals(1, reasons.size());
            Assert.assertFalse(reasons.get(0).isGeneralInterrogation());
            Assert.assertTrue(reasons.get(0).isDataChange());
        }
        else if (numReports >= 3) {
            List<BdaReasonForInclusion> reasons = report.getReasonCodes();
            Assert.assertEquals(2, reasons.size());
            Assert.assertTrue(reasons.get(0).isIntegrity());
            Assert.assertTrue(reasons.get(1).isIntegrity());
        }

        numSuccess++;

    }

    @Override
    public void associationClosed(IOException e) {
        System.out.println("Association closed!");
        numAssociationClosed++;
    }
}
