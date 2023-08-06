(ns mi-datni.backend.web.routes.app
  (:require
   [cheshire.core :as json]
   [hiccup.page :as hpage]
   [hiccup.util :refer [raw-string]]
   [hiccup2.core :as hiccup]
   [integrant.core :as ig]
   [mi-datni.backend.web.middleware.exception :as exception]
   [mi-datni.backend.web.middleware.formats :as formats]
   [clj-jwt.core :as jwt]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.http-response :as http-response]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.jwt :as m-jwt]
   [squint.compiler :as squint]
   [potpuri.core :as pot]
   [clojure.pprint :refer [pprint]]))

(defonce clerk-script-attributes (atom {}))

(def squint-core-js-url
  "https://cdn.jsdelivr.net/npm/squint-cljs@0.0.12/core.js")

(def squint-import
  [:script {:type "importmap"}
   (raw-string
    (json/encode
     {:imports {"squint-cljs/core.js" squint-core-js-url}}))])

(defn squint-script [squint-code]
  [:script {:type "module"}
   (raw-string
    (squint/compile-string
     (pr-str squint-code)))])

(defn html-page [body]
  (-> (hiccup/html
       (hpage/doctype :html5)
       [:html
        [:head
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
         [:script {:src "https://cdn.tailwindcss.com"}]
         [:script {:src "https://unpkg.com/htmx.org@1.8.6"}]
         squint-import]

        [:body.bg-gradient-to-b.from-indigo-800.to-indigo-950.min-h-screen.px-4.pt-4
         [:script @clerk-script-attributes]
         body]])
      str
      http-response/ok
      (http-response/content-type "text/html")))

(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance
   :middleware [wrap-cookies
                ;; query-params & form-params
                parameters/parameters-middleware
                ;; content-negotiation
                muuntaja/format-negotiate-middleware
                ;; encoding response body
                muuntaja/format-response-middleware
                ;; exception handling
                coercion/coerce-exceptions-middleware
                ;; decoding request body
                muuntaja/format-request-middleware
                ;; coercing response bodys
                coercion/coerce-response-middleware
                ;; coercing request parameters
                coercion/coerce-request-middleware
                ;; exception handling
                exception/wrap-exception]})

(def squint-code
  '(let [by-id #(document.getElementById %1)
         append! (fn [parent children]
                   (let [children (if (seqable? children) children [children])]
                     (doseq [c children]
                       (.appendChild parent c)))
                   nil)
         tag (fn [name children]
               (doto (document.createElement name)
                 (append! children)))
         text (fn [& xs]
                (document.createTextNode (apply str xs)))]
     (append! (by-id "main")
              (tag "p"
                   (tag "ul"
                        (for [i (range 10)]
                          (tag "li" (text "hello " i))))))))

(comment
  (println
   "\n----------------------------------------\n"
   (squint/compile-string
    (pr-str squint-code))))

(defn landing-page []
  [:div.pt-2.min-h-screen
   [:h1.text-white.text-center.font-bold.text-6xl "Mi Datni"]
   [:h2.text-white.text-center.font-bold.text-2xl
    [:span.text-green-300 "Visualize ..."]
    " Insights into Your Life Through "
    [:span.text-red-300 "Data Tracking"]]
   [:p.text-white.text-center.text-lg.mb-4
    "Your journey towards data-driven self-understanding starts here."]
   [:div.py-6.flex.items-center.justify-center.mt-12
    [:div.flex.flex-col.w-full.space-y-4.py-8.items-center.md:w-64.lg:w-96
     [:a.text-white.py-2.rounded.shadow-lg.bg-gradient-to-r.from-indigo-400.to-indigo-600.w-full.text-center.hover:from-indigo-300.hover:to-indigo-500
      {:href "/sign-up"} "Sign Up"]
     [:a.text-white.border-4.border-indigo-600.py-2.rounded.shadow-lg.w-full.text-center.hover:border-indigo-300
      {:href "/sign-in"} "Sign In"]]]])

(def squint-clerk-sign-up
  '(let [by-id #(document.getElementById %1)]
     (let [interval-id (atom nil)]
       (reset! interval-id
               (.setInterval js/window
                             (fn []
                               (when (window.Clerk.isReady)
                                 (window.Clerk.mountSignUp (by-id "sign-up"))
                                 (.clearInterval js/window @interval-id)))
                             100)))))

(defn sign-up-page []
  [:div.pt-2.min-h-screen.flex.items-center.justify-center
   (squint-script squint-clerk-sign-up)
   [:div {:id "sign-up"}]])

(def squint-clerk-sign-in
  '(let [by-id #(document.getElementById %1)]
     (let [interval-id (atom nil)]
       (reset! interval-id
               (.setInterval js/window
                             (fn []
                               (when (window.Clerk.isReady)
                                 (window.Clerk.mountSignIn (by-id "sign-in"))
                                 (.clearInterval js/window @interval-id)))
                             100)))))

(defn sign-in-page []
  [:div.pt-2.min-h-screen.flex.items-center.justify-center
   (squint-script squint-clerk-sign-in)
   [:div {:id "sign-in"}]])

;; Routes
(defn app-routes [_opts]
  [["/"        (fn [_]
                 (-> (landing-page) html-page))]
   ["/sign-up" (fn [_] (-> (sign-up-page) html-page))]
   ["/sign-in" (fn [_] (-> (sign-in-page) html-page))]
   ["/authed" {:middleware
               [#(m-jwt/wrap-jwt %
                                 {:issuers       {"https://relieved-bee-86.clerk.accounts.dev"
                                                  {:alg          :RS256
                                                   :jwk-endpoint "https://relieved-bee-86.clerk.accounts.dev/.well-known/jwks.json"}}
                                  :find-token-fn (fn [r]
                                                   (println "I'm finding the token!")
                                                   (println (-> r :cookies (get "__session") :value))
                                                   (-> r :cookies (get "__session") :value))})]}
    ["/" (fn [_]
           (-> [:div.flex.justify-end.p-3
                [:div.rounded-full.bg-white.p-1
                 [:div {:id "user-button"}]]
                (squint-script '(let [by-id #(document.getElementById %1)]
                                  (let [interval-id (atom nil)]
                                    (reset! interval-id
                                            (.setInterval js/window
                                                          (fn []
                                                            (when (window.Clerk.isReady)
                                                              (window.Clerk.mountUserButton (by-id "user-button"))
                                                              (.clearInterval js/window @interval-id)))
                                                          100)))))]
               html-page))]]])

(derive :reitit.routes/app :reitit/routes)

(defmethod ig/init-key :reitit.routes/app
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path route-data (app-routes opts)])

(defmethod ig/init-key :clerk/script-attr
  [_ attributes]
  (println (pot/map-of attributes))
  (reset! clerk-script-attributes attributes))
