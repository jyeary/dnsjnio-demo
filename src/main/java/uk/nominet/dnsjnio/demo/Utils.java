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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * @author Alex Dalitz <alex@caerkettontech.com>
 * @author John Yeary <jyeary@bluelotussoftware.com>
 * @version 1.0
 */
public class Utils {

    public static synchronized ArrayList<String> loadFile(String fileName) throws Exception {
        if ((fileName == null) || (fileName.isEmpty())) {
            throw new IllegalArgumentException();
        }

        String line;
        ArrayList<String> fileList = new ArrayList<>();
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            try {
                in = new BufferedReader(new FileReader("demo" + java.io.File.separator + fileName));
            } catch (FileNotFoundException ex) {
                throw new FileNotFoundException(MessageFormat.format("Can't find {0} or demo{1}{2}", fileName, File.separator, fileName));
            }
        }

        if (!in.ready()) {
            throw new IOException();
        }

        while ((line = in.readLine()) != null) {
            fileList.add(line);
        }

        in.close();
        return fileList;
    }

    
    public static synchronized Message makeQuery(String nameString, int id) throws TextParseException {
        Name name = Name.fromString(nameString, Name.root);
        Record question = Record.newRecord(name, Type.A, DClass.ANY);
        Message query = Message.newQuery(question);
        query.getHeader().setID(id);
        return query;
    }
    
}
