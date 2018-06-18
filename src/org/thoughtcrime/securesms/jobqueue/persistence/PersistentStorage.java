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

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.jobqueue.Job;
import org.thoughtcrime.securesms.jobqueue.dependencies.AggregateDependencyInjector;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PersistentStorage {

  private final Context                     context;
  private final JobSerializer               jobSerializer;
  private final AggregateDependencyInjector dependencyInjector;

  public PersistentStorage(Context context,
                           JobSerializer serializer,
                           AggregateDependencyInjector dependencyInjector)
  {
    this.context            = context;
    this.jobSerializer      = serializer;
    this.dependencyInjector = dependencyInjector;
  }

  public void store(Job job) throws IOException {
    String item = jobSerializer.serialize(job);
    long   id   = DatabaseFactory.getJobQueueDatabase(context).store(item);
    job.setPersistentId(id);
  }

  public List<Job> getJobs() {
    List<Job> results = new LinkedList<>();

    List<Pair<Long, String>> jobs = DatabaseFactory.getJobQueueDatabase(context).getJobs();

    for (Pair<Long, String> idItem : jobs) {
      try {
        Job job = jobSerializer.deserialize(idItem.second);

        job.setPersistentId(idItem.first);
        dependencyInjector.injectDependencies(context, job);

        results.add(job);
      } catch (IOException e) {
          Log.w("PersistentStore", e);
          remove(idItem.first);
      }
    }

    return results;
  }

  public void remove(long id) {
    DatabaseFactory.getJobQueueDatabase(context).remove(id);
  }

}
