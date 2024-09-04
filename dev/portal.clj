(ns portal)

(comment

  (require '[clojure.datafy :as d])
  (require '[portal.api :as p])

  (p/set-defaults!
    {:portal.launcher/port         1313
     :portal.launcher/window-title " portal"})

  (def submit (comp p/submit d/datafy))
  (add-tap #'submit)

  (p/open)
  (tap> *ns*)

  ())
