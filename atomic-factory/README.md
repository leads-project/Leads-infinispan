# Atomic Object Factory module

The class org.infinispan.AtomicObjectFactory implements a factory of atomic objects.
This factory is universal in the sense that it can instantiate an object of any (serializable) class
atop an Infinispan cache.

## Basic Usage

Below, we illustrate the usage of the AtomicObjectFactory class.

```
AtomicObjectFactory factory = new AtomicObjectFactory(c1); // c1 is both synchronous and transactional
Set set = (Set) factory.getOrCreateInstanceOf(HashSet.class, "k"); // k is the key to store set inside c1
set.add("something"); // some call examples
System.out.println(set.toString())
set.addAll(set);
factory.disposeInstanceOf(HashSet.class, "set", true); // to store in a persistent way the object
```

Additional examples are provided in org.infinispan.AtomicObjectFactoryTest.

## Limitations

The implementation requires that all the arguments of the methods of the object are Serializable, as well as the object itself.


## Implementation Details

We built the factory on top of the transactional facility of Infinispan.
In more details, when the object is created, we store both a local copy and a proxy registered as a cache listener.
We serialize every call in a transaction consisting of a single put operation.
When the call is de-serialized its applied to the local copy and, in case the calling process was local,
the tentative call value is returned (this mechanism is implemented as a future object).
