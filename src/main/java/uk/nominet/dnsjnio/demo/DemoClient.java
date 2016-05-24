/*
Copyright 2007 Nominet UK
Copyright 2016 Blue Lotus Software, LLC.

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
package uk.nominet.dnsjnio.demo;

import java.text.MessageFormat;
import java.util.ArrayList;
import org.xbill.DNS.*;
import uk.nominet.dnsjnio.*;

/**
 * This class acts as a simple demo for the dnsjnio extension. It loads the file
 * to_resolve.txt, and tries to resolve each of the names. Instead of a thread
 * (and socket) for each query, which dnsjava would do, the dnsjnio extension
 * runs all the queries in a single thread, over a single socket.
 *
 * @author Alex Dalitz <alex@caerkettontech.com>
 * @author John Yeary <jyeary@bluelotussoftware.com>
 *
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
        System.out.println("dnsjnio version: " + Version.VERSION);
        String name = filename;
        if (args.length == 1) {
            name = args[0];
        }
        ArrayList<String> toResolve = Utils.loadFile(name);
        resolver = new NonblockingResolver();
        resolver.setTimeout(30);
        resolver.setTCP(true);
        resolver.setSingleTcpPort(true);
        ResponseQueue responseQueue = new ResponseQueue();

        System.out.println("Sending all the queries asynchronously");
        long startTime = System.nanoTime();
        int ctr = 0;

        for (String unresolved : toResolve) {
            System.out.println("Querying for " + unresolved);
            try {
                Message message = Utils.makeQuery(unresolved, ctr);
                resolver.sendAsync(message, ctr, responseQueue);
                ctr++;
            } catch (TextParseException e) {
                System.out.println(MessageFormat.format("{0} is an invalid name.", unresolved));
            }
        }

        System.out.println("Sent " + ctr + " queries");
        // Now receive all the queries
        int goodCount = 0;
        int errorCount = 0;
        for (int i = 0; i < ctr; i++) {
            Response response = responseQueue.getItem();
            if (response.isException()) {
//                System.out.println("Got exception from " + toResolve.get(i) + ", error : " + response.getException().getMessage());
                errorCount++;
            } else {
//                System.out.println(toResolve.get(i) + " resolves to " + response.getMessage().getSectionRRsets(Section.ANSWER));
                goodCount++;
            }
        }

        long endTime = System.nanoTime();
        long time = ((endTime - startTime) / 1000000);

        System.out.println(MessageFormat.format("Received {0} responses, and {1} errors (most likely timeouts) in {2}ms", goodCount, errorCount, time));
        if (errorCount + goodCount < ctr) {
            System.out.println(MessageFormat.format("ERROR : {0} queries did not return!!", ctr - (errorCount + goodCount)));
        }
    }

}
