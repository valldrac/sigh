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

import java.io.IOException;

/**
 * A JobSerializer is responsible for serializing and deserializing persistent jobs.
 */
public interface JobSerializer {

  /**
   * Serialize a job object into a string.
   * @param job The Job to serialize.
   * @return The serialized Job.
   * @throws IOException if serialization fails.
   */
  public String serialize(Job job) throws IOException;

  /**
   * Deserialize a String into a Job.
   * @param serialized The serialized Job.
   * @return The deserialized Job.
   * @throws IOException If the Job deserialization fails.
   */
  public Job deserialize(String serialized) throws IOException;

}
