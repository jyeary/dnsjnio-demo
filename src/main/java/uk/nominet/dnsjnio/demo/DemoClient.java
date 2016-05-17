package uk.nominet.dnsjnio.demo;

/*
Copyright 2007 Nominet UK

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/

import org.xbill.DNS.*;
import uk.nominet.dnsjnio.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * This class acts as a simple demo for the dnsjnio extension.
 * It loads the file to_resolve.txt, and tries to resolve each
 * of the names. Instead of a thread (and socket) for each
 * query, which dnsjava would do, the dnsjnio extension runs
 * all the queries in a single thread, over a single socket.
 */
public class DemoClient {
    final String filename = "to_resolve.txt";
    NonblockingResolver resolver;

    public static void main(String[] args) throws Exception {
        DemoClient demo = new DemoClient();
        demo.demo(args);
    }

    public DemoClient() {
    }

    public void demo(String[] args) throws Exception {
        String name = filename;
        if (args.length == 1) {
            name = args[0];
        }
        ArrayList toResolve = loadFile(name);
        resolver = new NonblockingResolver();
        resolver.setTimeout(30);
        resolver.setTCP(true);
        resolver.setSingleTcpPort(true);
        ResponseQueue responseQueue = new ResponseQueue();
        // Send all the queries asynchronously
        for (int i = 0; i < toResolve.size(); i++) {
            String nextName = (String) (toResolve.get(i));
//            System.out.println("Querying for " + nextName);
            Integer id = new Integer(i);
            resolver.sendAsync(makeQuery(nextName, i), id, responseQueue);
        }
        System.out.println("Sent " + toResolve.size() + " queries");
        // Now receive all the queries
        int goodCount = 0;
        int errorCount = 0;
        for (int i = 0; i < toResolve.size(); i++) {
            Response response = responseQueue.getItem();
            if (response.isException()) {
//                System.out.println("Got exception from " + toResolve.get(i) + ", error : " + response.getException().getMessage());
                errorCount++;
            } else {
//                System.out.println(toResolve.get(i) + " resolves to " + response.getMessage().getSectionRRsets(Section.ANSWER));
                goodCount++;
            }
        }
        System.out.println("Received " + goodCount + " responses, and " + errorCount + " errors (most likely timeouts)");
        if (errorCount + goodCount < toResolve.size()) {
            System.out.println("ERROR : " + (toResolve.size() - (errorCount + goodCount)) + " queries did not return!!");
        }
    }

    private Message makeQuery(String nameString, int id) throws TextParseException {
        Name name = Name.fromString(nameString, Name.root);
        Record question = Record.newRecord(name, Type.A, DClass.ANY);
        Message query = Message.newQuery(question);
        query.getHeader().setID(id);
        return query;
    }

    public ArrayList loadFile(String fileName) throws Exception {
        if ((fileName == null) || (fileName == ""))
            throw new IllegalArgumentException();

        String line;
        ArrayList fileList = new ArrayList();
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(fileName));
        }
        catch (FileNotFoundException e) {
        	try {
            in = new BufferedReader(new FileReader("demo" + java.io.File.separator + fileName));
        	}
        	catch (FileNotFoundException ex) {
        		throw new FileNotFoundException("Can't find " + fileName + " or demo" + java.io.File.separator + fileName);
        	}
        }

            if (!in.ready())
                throw new IOException();

            while ((line = in.readLine()) != null) fileList.add(line);

            in.close();
        return fileList;
    }
}
