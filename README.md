# Bonsai
A miniature reactive Java library - for Android.

### How to use

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
