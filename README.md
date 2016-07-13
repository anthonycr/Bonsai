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
    - `onStart()`: Always called when the observable starts (called internally)
    - `onNext(T item)`: Called by the Observable if it has an item to pass to you
    - `onComplete()`: Should be called by the Observable when it is done emitting items, this will release the resources used by the Observable/Subscription unless they are held elsewhere. Not calling this indicates that the Observable has more items to emit.
    - `onError(Exception exception)`: TODO: This API needs work and will likely be changed to be called automatically when you throw an exception. Currently: The Observable should call this if an error occurs. If this method is called, onComplete or onNext should not be called. 
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
  .subscribe(new OnSubscribe<String>() {
        @Override
        public void onNext(String item) {
            Log.d(TAG, "Asynchronously received this string: " + item);
        }

        @Override
        public void onComplete() {
            Log.d(TAG, "Finished receiving strings");
        }
  });
```

##### Unsubscribe example
```java
private Subscription subscription;

/**
 * An observable that emits sequential Fibonacci numbers as long
 * as the Subscriber is subscribed to it. Fibonacci numbers are
 * Emitted every half a second.
 */
private Observable<Integer> allFibonacciNumbersObservable() {
    return Observable.create(new Action<Integer>() {
        @Override
        public void onSubscribe(@NonNull Subscriber<Integer> subscriber) {
            int firstNumber = 0;
            int secondNumber = 1;
            int temp;
            subscriber.onNext(secondNumber);
            while(!subscriber.isUnsubscribed()) {
                temp = secondNumber;
                secondNumber = secondNumber + firstNumber;
                firstNumber = temp;
                subscriber.onNext(secondNumber);
                try {
                    Thread.sleep(500);
                } catch(InterruptedException exception) {
                    subscriber.onError(exception);
                    return;
                }
            }
            subscriber.onComplete();
        }
    });
}

private void doWorkOnMainThread() {
    subscription = allFibonacciNumbersObservable()
        .subscribeOn(Schedulers.worker())
        .observeOn(Schedulders.main())
        .subscribe(new OnSubscribe<Integer>() {
            @Override
            public void onStart() {
                Log.d(TAG, "Started receiving numbers");
            }

            @Override
            public void onNext(Integer item) {
                Log.d(TAG, "Asynchronously received this fibonacci number: " + item);
            }

            @Override
            public void onError(Exception error) {
                Log.d(TAG, "Error occurred while receiving numbers", error);
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

##### List Example
```java
List<String> list = new ArrayList();
Observable.create(new Action<List<String>>() {
    @Override
    public void onSubscribe(@NonNull Subscriber<List<String>> subscriber) {
        List<String> stringList = new Arraylist<>();
        stringList.add("string 1");
        stringList.add("string 2");
        stringList.add("string 3");
        subscriber.onNext(stringList);
        subscriber.onComplete();
    }
}).subscribeOn(Schedulers.current())
  .observeOn(Schedulers.current())
  .subscribe(new OnSubscribe<List<String>>() {
        @Override
        public void onNext(List<String> item) {
            list.addAll(item);
        }
        
        @Override
        public void onComplete() {
            Log.d(TAG, "We're done!");
        }
  });
  
for(String string : list) {
    Log.d(TAG, "Received: " + string);
}
```


### License

````
Copyright 2016 Anthony Restaino

Licensed under the Apache License, Version 2.0 (the "License"); you may 
not use this file except in compliance with the License. You may obtain 
a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
License for the specific language governing permissions and limitations 
under the License.
````
