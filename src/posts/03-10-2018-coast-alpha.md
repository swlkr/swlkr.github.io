---
title: coast.alpha: an easy way to make websites with clojure
preview: Hot on the heels of coast 1.0 comes coast.alpha, why dot alpha and not 2.0? Because I hate you. No I'm kidding, I don't hate you! I really did it because...
date: March 10 2018
---

Hot on the heels of coast 1.0 comes coast.alpha, why dot alpha and not 2.0?

Because I hate you.

No I'm kidding, I don't hate you! I really did it because there are so many new, incompatible changes, I'd rather just keep 1.0 going, still work on bug fixes and not pretend like you can just upgrade and have everything work out. If you try to upgrade from coast to coast.alpha, you aren't going to have a good time because they aren't the same! The name has changed! Haha!

With that out of the way, let's look at what's new:

**No more leiningen**

It's a scary, brave new world out there in Clojure tooling land, but luckily, Clojure 1.9.0, tools.deps and makefiles have your back

**Easier namespace references**

I realized that since I'm making websites with coast, not libraries, there's no point to scatter the project name across every freakin namespace, just reference things from src/ and that's that

**Simpler and more explicit resource routing**

This was mostly a library change, but it's worth mentioning, no more keywords, just symbols.

**Simpler models**

No more dynamic sql generation at all, every bit of sql is static and beautiful, with generators to help out for the inserts, updates and deletes.

**Less dependencies**

I've basically inlined everything that was a separate library before. This makes it so much easier to make underlying changes

**A worker process**

The one thing coast was missing was a worker process and background jobs, it has it now, although it's kind of weird, just like coast itself, so hopefully someone can either give me a much better solution or just embrace the weird

There are probably other changes, mostly under the hood and the way the generators work since there's no more lein, but it's pretty straightforward... hopefully. Anyway, give it a star on github or at the very least check it out and see if it works for you!
