/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.jobqueue.persistence;

import org.thoughtcrime.securesms.jobqueue.Job;
import org.thoughtcrime.securesms.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An implementation of {@link org.thoughtcrime.securesms.jobqueue.persistence.JobSerializer} that uses
 * Java Serialization.
 */
public class JavaJobSerializer implements JobSerializer {

  public JavaJobSerializer() {}

  @Override
  public String serialize(Job job) throws IOException {
    return Base64.encodeObject(job);
  }

  @Override
  public Job deserialize(String serialized) throws IOException {
    try {
      return (Job)Base64.decodeToObject(serialized);
    } catch (ClassNotFoundException e) {
      StringWriter sw = new StringWriter();
      PrintWriter  pw = new PrintWriter(sw);
      e.printStackTrace(pw);

      throw new IOException(e.getMessage() + "\n" + sw.toString());
    }
  }
}
