/*
 * Copyright 2016 Blue Lotus Software, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.nominet.dnsjnio.demo;

import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.xbill.DNS.Message;
import uk.nominet.dnsjnio.NonblockingResolver;
import uk.nominet.dnsjnio.Response;
import uk.nominet.dnsjnio.ResponseQueue;

/**
 *
 * @author John Yeary <jyeary@bluelotussoftware.com>
 * @version 1.0
 */
public class UDPExecutorServiceDemo {

    private final ConcurrentMap<Integer, Message> messages;
    private final ConcurrentMap<Integer, Response> responses;
    private final ExecutorService incomingExecutorService;
    private final NonblockingResolver resolver;
    private final ResponseQueue responseQueue;
    private final AtomicInteger counter;
    private final AtomicInteger goodCounter;
    private final AtomicInteger badCounter;

    public UDPExecutorServiceDemo(final String hostname) throws UnknownHostException {
        resolver = new NonblockingResolver(hostname);
        resolver.setTCP(false);
        resolver.setUseSingleUDPPort(true);
        resolver.setTimeout(30);

        incomingExecutorService = Executors.newFixedThreadPool(10);
        responseQueue = new ResponseQueue();
        counter = new AtomicInteger();
        goodCounter = new AtomicInteger();
        badCounter = new AtomicInteger();
        messages = new ConcurrentHashMap<>(50000);
        responses = new ConcurrentHashMap<>(50000);
    }

    public UDPExecutorServiceDemo() throws UnknownHostException {
        this(null);
    }

    public static void main(String[] args) throws Exception {
        String filename = "to_resolve_50k.txt";
        UDPExecutorServiceDemo demo = new UDPExecutorServiceDemo();

        if (args.length == 1) {
            filename = args[0];
        }

        List<String> toResolve = Utils.loadFile(filename);
        for (String name : toResolve) {
            Message message = Utils.makeQuery(name, demo.getCounter().getAndIncrement());
            demo.getMessages().put(message.getHeader().getID(), message);
            demo.getResolver().sendAsync(message, demo.getResponseQueue());
        }
        System.out.println(MessageFormat.format("Sending {0} queries asynchronously", demo.getCounter().get()));

        //Setting the timeout to 2 x the resolver timeout.
        long timeout = System.currentTimeMillis() + (demo.getResolver().getTimeoutMillis() * 2);
        while (System.currentTimeMillis() < timeout) {
            demo.getIncomingExecutorService().submit(new UDPReceiver(demo.getResponseQueue(),
                    demo.getGoodCounter(), demo.getBadCounter(), demo.getMessages(), demo.getResponses()));
        }

        //TODO We could do something with the results at this point, or requeue messages that failed.
        
        demo.generateStatistics();

        demo.getIncomingExecutorService().shutdownNow();
        System.out.println("Done....");

    }

    public void generateStatistics() {
        int total_responses = goodCounter.get() + badCounter.get();
        int failures = counter.get() - total_responses;
        System.out.println(MessageFormat.format("Good {0} Bad: {1} Total Reponses: {2} Failures {3}", goodCounter.get(), badCounter.get(), total_responses, failures));
    }

    public NonblockingResolver getResolver() {
        return resolver;
    }

    public ResponseQueue getResponseQueue() {
        return responseQueue;
    }

    public ExecutorService getIncomingExecutorService() {
        return incomingExecutorService;
    }

    public AtomicInteger getCounter() {
        return counter;
    }

    public ConcurrentMap<Integer, Message> getMessages() {
        return messages;
    }

    public ConcurrentMap<Integer, Response> getResponses() {
        return responses;
    }

    public AtomicInteger getGoodCounter() {
        return goodCounter;
    }

    public AtomicInteger getBadCounter() {
        return badCounter;
    }

}

class UDPReceiver implements Runnable {

    private final ResponseQueue queue;
    private final AtomicInteger good;
    private final AtomicInteger bad;
    private final ConcurrentMap<Integer, Message> messages;
    private final ConcurrentMap<Integer, Response> responses;

    public UDPReceiver(final ResponseQueue queue, final AtomicInteger good, final AtomicInteger bad,
            final ConcurrentMap<Integer, Message> messages, ConcurrentMap<Integer, Response> responses) {
        this.queue = queue;
        this.good = good;
        this.bad = bad;
        this.messages = messages;
        this.responses = responses;
    }

    @Override
    public void run() {
        Response response = queue.getItem();
        if (response.isException()) {
            bad.incrementAndGet();
        } else {
            good.incrementAndGet();
        }
        messages.remove((Integer) response.getId());
        responses.put((Integer) response.getId(), response);
    }

}
