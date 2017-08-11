(ns mango.hydrate-test
  (:require [clojure.test :refer :all]
            [mango.auth :refer [public-user]]
            [mango.hydrate :refer :all]
            [mango.fixtures :as fixtures]))

(deftest test-hydrate-user
  (is (= (user fixtures/data-provider {}) {}))
  (is (= (user fixtures/data-provider {:user 1234}) {:user (public-user fixtures/user)}))
  (is (= (user fixtures/data-provider {:user 1234 :other-stuff "Untouched"}) {:user (public-user fixtures/user) :other-stuff "Untouched"})))

(deftest test-hydrate-media
  (is (= (media fixtures/data-provider {}) {}))
  (is (= (media fixtures/data-provider {:media [1 2 3 4]}) {:media [fixtures/media1 fixtures/media2 fixtures/media3 fixtures/media4]})))

(deftest test-hydrate-content
  (is (= (content {}) {}))
  (is (= (content {:content "Hi"})
         {:content "Hi"
          :rendered-content "<p>Hi</p>"}))
  (is (= (content {:content "```asm\nmov #0x19, r13\n```"})
         {:content "```asm\nmov #0x19, r13\n```"
          :rendered-content "<pre><code class=\"asm\">mov #0x19, r13\n</code></pre>"})))

(deftest test-hydrate-article
  (is (= (article fixtures/data-provider fixtures/article) fixtures/hydrated-article)))

(deftest test-hydrate-articles
  (is (= (articles fixtures/data-provider [fixtures/article fixtures/article2])
         [fixtures/hydrated-article fixtures/hydrated-article2])))
