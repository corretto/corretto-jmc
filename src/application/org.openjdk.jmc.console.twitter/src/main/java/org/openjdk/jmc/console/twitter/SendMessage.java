/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.console.twitter;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.triggers.IActivatableTriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerEvent;

/**
 * Sends a direct message from one user to another
 */
public class SendMessage extends TriggerAction implements IActivatableTriggerAction {
	private static final String FROM_FIELD = "from"; //$NON-NLS-1$
	private static final String TO_FIELD = "to"; //$NON-NLS-1$
	private static final String MESSAGE_FIELD = "message"; //$NON-NLS-1$

	public SendMessage() {

	}

	@Override
	public void handleNotificationEvent(TriggerEvent event) throws Exception {
		String from = getSetting(FROM_FIELD).getString();
		String to = getSetting(TO_FIELD).getString();
		String message = TwitterPlugin.createMessage(getSetting(MESSAGE_FIELD).getString(), event);

		if (from != null && to != null && message != null) {
			send(from, to, message);
		}
	}

	private void send(String from, String to, String message) throws Exception {
		TwitterPlugin.getDefault().sendDirectMessage(from, to, message);
	}

	@Override
	public boolean isActivatable(IConnectionHandle handle) {
		return TwitterPlugin.getDefault().verifyAuthorizedUser(getSetting(FROM_FIELD).getString());
	}
}
