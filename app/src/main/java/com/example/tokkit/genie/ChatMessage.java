// ---------------------------------------------------------------------
// Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
// SPDX-License-Identifier: BSD-3-Clause
// ---------------------------------------------------------------------
package com.example.tokkit.genie;

/**
 * ChatMessage: Holds information about each message within Chat
 */
public class ChatMessage {

    public String mMessage;
    public int mLength;
    public com.example.tokkit.genie.MessageSender mSender;

    public ChatMessage(String msg,com.example.tokkit.genie.MessageSender sender) {
        mMessage = msg;
        mLength = msg.length();
        mSender = sender;
    }

    public boolean isMessageFromUser() {
        return mSender == com.example.tokkit.genie.MessageSender.USER;
    }

    public String getMessage() {
        return mMessage;
    }
}
