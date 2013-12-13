/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */

package org.opends.quicksetup.event;

import org.opends.messages.Message;
import org.opends.quicksetup.ProgressStep;

/**
 * The event that is generated when there is a change during the installation
 * process (we get a new log message when starting the server, or we finished
 * configuring the server for instance).
 *
 * In the current implementation this events are generated by the Installer
 * objects and are notified to the objects implementing
 * ProgressUpdateListener (QuickSetup object).
 *
 */
public class ProgressUpdateEvent {
  private ProgressStep step;

  private Integer progressRatio;

  private Message currentPhaseSummary;

  private Message newLogs;

  /**
   * Constructor of the ProgressUpdateEvent.
   * @param step the ProgressStep object describing in which step
   * of the installation we are (configuring server, starting server, etc.)
   * @param progressRatio the integer that specifies which percentage of
 * the whole installation has been completed.
   * @param currentPhaseSummary the localized summary message for the
* current installation progress.
   * @param newLogs the new log messages that we have for the installation.
   */
  public ProgressUpdateEvent(ProgressStep step,
      Integer progressRatio, Message currentPhaseSummary, Message newLogs)
  {
    this.step = step;
    this.progressRatio = progressRatio;
    this.currentPhaseSummary = currentPhaseSummary;
    this.newLogs = newLogs;
  }

  /**
   * Gets a localized message summary describing the install progress
   * status.
   * @return the localized message summary describing the progress status.
   */
  public Message getCurrentPhaseSummary()
  {
    return currentPhaseSummary;
  }

  /**
   * Gets the new logs for the install progress.
   * @return the new logs for the current install progress.
   */
  public Message getNewLogs()
  {
    return newLogs;
  }

  /**
   * Gets the progress ratio for the install progress.
   * @return the progress ratio for the install progress.
   */
  public Integer getProgressRatio()
  {
    return progressRatio;
  }

  /**
   * Gets the current progress step.
   * @return the current progress step.
   */
  public ProgressStep getProgressStep()
  {
    return step;
  }
}
