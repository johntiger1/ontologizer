package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MessageEvent;

/**
 * A simple Java wrapper for Javascript Worker.
 *
 * @author Sebastian Bauer
 */
public abstract class Worker implements JSObject, EventTarget
{
	/**
	 * @return the current worker.
	 */
	@JSBody(script="return worker", params = {})
	public static native Worker current();

	/**
	 * Creates a new worker.
	 * @param name to the Javascript file to be executed by the worker.
	 * @return an instance of the worker.
	 */
	@JSBody(script="return new Worker(name);", params={"name"})
	public static native Worker create(String name);

	private void listenMessage(EventListener<MessageEvent> listener)
	{
		addEventListener("message", listener);
	}

	/**
	 * Listen on a particular worker message.
	 *
	 * @param cl the class of the worker message to listen.
	 * @param receiver the handler of the worker message of the given class.
	 */
	public <T extends WorkerMessage> void listenMessage(Class<T> cl, WorkerMessageHandler<T> receiver)
	{
		listenMessage((ev)->{
			T wm = ev.getData().cast();
			if (wm.getType().equals(cl.getName()))
			{
				receiver.handle(wm);
			}
		});
	}

	/**
	 * Listen on a particular replyable worker message. This will return the listened message to the receiver
	 * together with the result once it has been handled.
	 *
	 * @param cl the class of the worker message to listen.
	 * @param receiver the handler of the worker message of the given class.
	 *  The result of it is replied together with the original message to the
	 *  originator.
	 */
	public <R extends JSObject,T extends ReplyableWorkerMessage<R>> void listenMessage2(Class<T> cl, WorkerMessageHandlerWithResult<R, T> receiver)
	{
		listenMessage((ev)->{
			T wm = ev.getData().cast();
			if (wm.getType().equals(cl.getName()))
			{
				R result = receiver.handle(wm);
				wm.setResult(result);
				postMessage(wm);
			}
		});
	}

	/**
	 * Post a message to the worker.
	 *
	 * @param obj the message payload.
	 */
	public abstract void postMessage(JSObject obj);

	/**
	 * Post a replyable message to the worker. A replyable message is a message that is replied by the
	 * worker with an additional result.
	 *
	 * @param cl the class of the worker message to post.
	 * @param message the message to post
	 * @param whenDone definines the action on the context of the caller to be exectuted when the message
	 *  was replied.
	 */
	public <R extends JSObject, T extends ReplyableWorkerMessage<R>> void postMessage(Class<T> cl, T message, final IWhenDone<R> whenDone)
	{
		/* FIXME: Once replied, we should remove that listener again (or use manage it
		 * on our own)
		 */
		listenMessage(cl, (T m) ->
		{
			if (m.getId() == message.getId())
			{
				whenDone.done(m.getResult());
			}
		});
		postMessage(message);
	}
}
