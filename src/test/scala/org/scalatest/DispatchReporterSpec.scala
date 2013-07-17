/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest

import java.io.PrintStream
import java.io.ByteArrayOutputStream
import org.scalatest.events._
import SharedHelpers._
import concurrent.Eventually._
import time.SpanSugar._

class DispatchReporterSpec extends Spec with Matchers {
  object `the DispatchReporter` {
    object `when slowpoke detection is enabled` {
      def fireTestStarting(): (EventRecordingReporter, DispatchReporter) = {
        val erp = new EventRecordingReporter
        val dispatch = new DispatchReporter(List(erp, NoisyReporter), Console.err, true, 1, 1)
        dispatch(
          TestStarting(
            ordinal = new Ordinal(223),
            suiteName = "the suite name",
            suiteId = "the suite ID",
            suiteClassName = Some("suiteClassName"),
            testName = "the test name",
            testText = "test name"
          )
        )
        (erp, dispatch)
      }
      def `should send out InfoProvided events if a slowpoke is detected` {
        val (erp, dispatch) = fireTestStarting()
        eventually {
          erp.infoProvidedEventsReceived.size should be > 0
        }
        dispatch.doDispose()
      }
      def doTestStartingAndFinishedEvents(testFinishedEvent: Event): Unit = {
        val (erp, dispatch) = fireTestStarting()
        eventually {
          erp.infoProvidedEventsReceived.size should be > 0
        }
        dispatch(testFinishedEvent)
        var sizeWasSameCount = 0
        var previousSize = erp.infoProvidedEventsReceived.size
        eventually {
          val size = erp.infoProvidedEventsReceived.size 
          if (size == previousSize)
            sizeWasSameCount += 1
          else
            sizeWasSameCount = 0 
          previousSize = size
          sizeWasSameCount should be >= 5
        }
        dispatch.doDispose()
      }
      def `should stop sending out InfoProvided events after a detected slowpoke succeeds` {
        doTestStartingAndFinishedEvents(
          TestSucceeded(
            ordinal = new Ordinal(223),
            suiteName = "the suite name",
            suiteId = "the suite ID",
            suiteClassName = Some("suiteClassName"),
            testName = "the test name",
            testText = "test name",
            recordedEvents = collection.immutable.IndexedSeq.empty
          )
        )
      }
      def `should stop sending out InfoProvided events after a detected slowpoke fails` {
        doTestStartingAndFinishedEvents(
          TestFailed(
            ordinal = new Ordinal(223),
            message = "I meant to do that!",
            suiteName = "the suite name",
            suiteId = "the suite ID",
            suiteClassName = Some("suiteClassName"),
            testName = "the test name",
            testText = "test name",
            recordedEvents = collection.immutable.IndexedSeq.empty
          )
        )
      }
    }
  }
}
