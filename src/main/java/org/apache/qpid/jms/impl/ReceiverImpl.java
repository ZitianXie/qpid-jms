/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.jms.impl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.qpid.jms.engine.AmqpMessage;
import org.apache.qpid.jms.engine.AmqpReceiver;

public class ReceiverImpl extends LinkImpl implements MessageConsumer
{
    private final AmqpReceiver _amqpReceiver;
    private final SessionImpl _sessionImpl;

    public ReceiverImpl(ConnectionImpl connectionImpl, SessionImpl sessionImpl, AmqpReceiver amqpReceiver)
    {
        super(connectionImpl, amqpReceiver);
        _sessionImpl = sessionImpl;
        _amqpReceiver = amqpReceiver;
    }

    @Override
    public Message receive() throws JMSException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public Message receive(long timeout) throws JMSException
    {
        getConnectionImpl().lock();
        try
        {
            if(!getConnectionImpl().isStarted())
            {
                return null;
            }

            MessageReceivedPredicate messageReceievedCondition = new MessageReceivedPredicate();
            getConnectionImpl().waitUntil(messageReceievedCondition, timeout);

            //TODO: decide what if any particular message impl class to instantiate

            AmqpMessage receivedAmqpMessage = messageReceievedCondition.getReceivedMessage();
            MessageImpl receivedMessageImpl = new MessageImpl(receivedAmqpMessage, _sessionImpl, getConnectionImpl());

            //TODO: accepting/settling will be acknowledge-mode dependent
            if(_sessionImpl.getAcknowledgeMode() == Session.AUTO_ACKNOWLEDGE)
            {
                receivedAmqpMessage.accept(true);
            }
            else
            {
                throw new UnsupportedOperationException("Only Auto-Ack currently supported");
            }

            getConnectionImpl().stateChanged();

            return receivedMessageImpl;
        }
        catch (JmsTimeoutException e)
        {
            //No message in allotted time, return null to signal this
            return null;
        }
        finally
        {
            getConnectionImpl().releaseLock();
        }
    }

    public void credit(int credit)
    {
        getConnectionImpl().lock();
        try
        {
            _amqpReceiver.credit(credit);
            getConnectionImpl().stateChanged();
        }
        finally
        {
            getConnectionImpl().releaseLock();
        }
    }

    private final class MessageReceivedPredicate extends SimplePredicate
    {
        AmqpMessage _message;

        public MessageReceivedPredicate()
        {
            super("Message received", _amqpReceiver);
        }

        @Override
        public boolean test()
        {
            if(_message == null)
            {
                _message = _amqpReceiver.receiveNoWait();
            }
            return _message != null;
        }

        public AmqpMessage getReceivedMessage()
        {
            return _message;
        }
    }

    @Override
    public String getMessageSelector() throws JMSException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public MessageListener getMessageListener() throws JMSException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public Message receiveNoWait() throws JMSException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

}
