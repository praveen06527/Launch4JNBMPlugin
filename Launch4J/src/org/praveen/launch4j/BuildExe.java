/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.praveen.launch4j;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.openide.loaders.DataObject;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@ActionID(
        category = "Build",
        id = "org.praveen.launch4j.BuildExe"
)
@ActionRegistration(
        iconBase = "org/praveen/launch4j/launch4j_icon.png",
        displayName = "#CTL_BuildExe"
)
@ActionReference(path = "Loaders/application/x-java-archive/Actions", position = -50, separatorBefore = -100, separatorAfter = 0)
@Messages("CTL_BuildExe=Build EXE")
public final class BuildExe implements ActionListener {
    
    private final DataObject context;

    public BuildExe(DataObject context) {
        this.context = context;
        System.setOut(new PrintStream(new TextAreaOutputStream(BuildEXEOutput.logger)));
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        try {
            FileObject jarFile = context.getPrimaryFile();
            JarInputStream jis = new JarInputStream(new FileInputStream(jarFile.getPath()));
            final String buildDir = jarFile.getParent().getPath();
            final File launch4jConfig = new File(buildDir + "/config.xml");
            Attributes attributes = jis.getManifest().getMainAttributes();
            InputStream config = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            if(launch4jConfig.exists()){
                config=new FileInputStream(launch4jConfig);
            }else{
                config = BuildExe.class.getResourceAsStream("/config.xml");
            }
            Document document = builder.parse(new InputSource(config));
            String projName = jarFile.getName();
            changeXMLTagValue(document, "outfile", projName + ".exe");
            changeXMLTagValue(document, "jar", jarFile.getNameExt());
            changeXMLTagValue(document, "mutexName", projName);
            changeXMLTagValue(document, "windowTitle", projName);
            changeXMLTagValue(document, "fileVersion", attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
            changeXMLTagValue(document, "txtFileVersion", attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
            changeXMLTagValue(document, "productVersion", attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
            changeXMLTagValue(document, "txtProductVersion", attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
            changeXMLTagValue(document, "productName", attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE));
            changeXMLTagValue(document, "companyName", attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR));
            changeXMLTagValue(document, "originalFilename", projName + ".exe");
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(launch4jConfig);
            transformer.transform(source, result);
            Runtime rt = Runtime.getRuntime();
            final Process p = rt.exec("launch4j.exe " + buildDir + "/config.xml");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    java.io.BufferedReader input = new java.io.BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line = null;
                    try {
                        while ((line = input.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }).start();
            p.waitFor();
        } catch (Exception ex) {
            System.out.println("Error:"+ex.getMessage());
            System.out.println("Please add launch4j.exe to Path environment variable and restart the IDE");
        } finally {
        }
    }

    public void changeXMLTagValue(Document document, String tagName, String value) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes != null && nodes.item(0) != null) {
            nodes.item(0).setTextContent(value);
        } else {
            System.out.println("Unable to find tag:" + tagName);
        }
    }
}
