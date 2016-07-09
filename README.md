# Bonsai
A miniature reactive Android library.

### Why
[RxJava](https://github.com/ReactiveX/RxJava) is a great library that introduces the concept of reactive programming to your Android code. However, to newcomers, it can be a daunting library with a high learning curve. You have to learn about Schedulers, Observables, Singles, Subscribers, Subscriptions, and many more paradigms that can be confusing to someone new to reactive programming. Additionally, for the Android developer, it can be confusing as Rx is primarily a Java library, and there are some things on Android that are functionally different (e.g. the main thread). To use Rx in an Android app properly, you also need to use [RxAndroid](https://github.com/ReactiveX/RxAndroid). The relationship between the two can be confusing. The aim of Bonsai is to help people interested in using reactive Java in their Android code be introduced to some of the basic concepts of Rx. Bonsai mirrors some of the basic parts of Rx in a way that users of the library can understand. Since there is very little source code compared to the more functional Rx library, it will be easier for users to dive in and see how something works. Additionally, Bonsai has built in support for the Android main thread, which should help users adopt functional programming into their code. Hopefully some of you find this library helpful. This is not meant to be an Rx replacement, but rather a taste of what the reactive world has to offer. In case you are interested, I am currently using this in production code.

### Usage

`compile project(':library')`

### The API

- `Observable`: A manager class that handles communication between an `Action` and a `Subscriber` by scheduling events to occurr on different threads using a `Scheduler`
- `Action`: Work that you wish to perform when the user subscribes to your observable. `Action.onSubscribe` is called when the user subscribes to the Observable. The work is run on the Scheduler specified by the `Observable.subscribeOn` method.
- `Subscriber`, `OnSubscribe`: The consumer of the observable is the Subscriber. There are two classes describing this in order to separate communication between the subscriber thread (where the work is done) and the observer thread (where it is emitted). Subscriber is used by the subscriber thread to pass events to the OnSubscribe implementation (provided by ths subscriber), which is called on the observe thread.
- `Scheduler`: A thin wrapper around a thread that schedules work to be done.
- `Schedulers`: A utility class that creates `Scheduler` instances for you. See below for a list of provided ones:
    - `io()`: A single thread that you should reserve for talking to disk. Android disk IO is single threaded for write operations, which is why one thread is used here.
    - `main()`: A reference to the main thread of your application. All work will be posted to the main message queue.
    - `worker()`: Runs work on one thread out of a pool.
    - `newSingleThreadScheduler()`: Creates a new single thread scheduler (like the `io()` scheduler).
    - `current()`: Runs work on the thread that this method was called on.
    - `from(Executor)`: Allows you to construct a scheduler backed by an `Executor` of your choice.
- `Subscription`: This is returned when you subscribe to an Observable. It allows you to unsubscribe from the work. If you unsubscribe, you will no longer receive events. This is especially helpful in Android if we are observing long lasting work in an Activity, we want to unsubscribe in `Activity.onDestroy()` in order to avoid leaking the Activity.

### How to use

##### Basic example
```java
Observable.create(new Action<String>() {
    @Override
    public void onSubscribe(@NonNull Subscriber<String> subscriber) {
        subscriber.onNext("string 1");
        subscriber.onNext("string 2");
        subscriber.onNext("string 3");
        subscriber.onComplete();
    }
}).subscribeOn(Schedulers.io())
  .observeOn(Schedulers.main())
  .subscribe(new OnSubscribe<String>(){
        @Override
        public void onNext(String item) {
            Log.d(TAG, "Asynchronously received this string: " + item);
        }
  });
```

##### Unsubscribe example
```java
private Subscription subscription;

/**
 * An observable that emits sequential odd numbers as long
 * as the Subscriber is subscribed to it. Odd numbers are
 * Emitted every half a second.
 */
private Observable<Integer> allPositiveOddNumbersObservable() {
    Observable.create(new Action<Integer>() {
        @Override
        public void onSubscribe(@NonNull Subscriber<Integer> subscriber) {
            int oddNumber = -1;
            while(!subscriber.isUnsubscribed()) {
                oddNumber += 2;
                subscriber.onNext(oddNumber);
                try {
                    Thread.sleep(500);
                } catch(InterruptedException ignored) {}
            }
            subscriber.onComplete();
        }
    });
}

private void doWorkOnMainThread() {
    subscription = stringObservable()
        .subscribeOn(Schedulers.worker())
        .observeOn(Schedulders.main())
        .subscribe(new OnSubscribe<Integer>() {
            @Override
            public void onNext(Integer item) {
                Log.d(TAG, "Asynchronously received this odd number: " + item);
            }
        });
}

@Override
public void onDestroy() {
    super.onDestroy();
    if (subscription != null) {
        subscription.unsubscribe();
    }
}
```
