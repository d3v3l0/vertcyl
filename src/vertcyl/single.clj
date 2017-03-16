(ns vertcyl.single
  (:require [scad-clj.model :refer [cube cylinder difference rotate translate
                                    union scale hull *fn* sphere with-fn]]
            [scad-clj.scad :refer [write-scad]]))

(def width 21)
(def height 21)
(def radius 110)
(def plate-thickness 3)
(def wall-thickness 10)
(def finger-rows 4)
(def finger-columns 5)
(def thumb-upper-rows 3)
(def thumb-lower-rows 2)
(def cirum (* Math/PI 2 radius))
(def step-u10 (/ (* Math/PI 2) (/ cirum width)))
(def step-u15 (/ (* Math/PI 2) (/ cirum (* width 1.5))))
(def offset-u10 (/ step-u10 2))
(def offset-u15 (/ step-u15 2))
(def screw-radius 1)
(def mounting-post-height 30)
(def mounting-post-radius 3)
(def shell-thickness 15)
(def shell-height 90)
(def thumb-x -30)
(def thumb-y 35)
(def thumb-z 20)
(def shell-gap 10)

(def switch-cutter
  (let [extra (cube 3.5 15.6 40)
        angle (/ Math/PI 2)]
    (->> (union
           (cube 14 14 40)
           (translate [4.0 0 0] extra)
           (translate [-4.0 0 0] extra)
           (->> extra
                (rotate angle [0 0 1])
                (translate [0 4 0]))
           (->> extra
                (rotate angle [0 0 1])
                (translate [0 -4 0])))
         (rotate angle [-1 0 0]))))

(def wall-plate-u10
  (->> (cube (+ width plate-thickness) (+ height plate-thickness) wall-thickness)
       (rotate (/ Math/PI 2) [-1 0 0])
       (translate [0 (- (/ wall-thickness 2)) 0])))

(def wall-plate-u15
  (->> (cube (+ width plate-thickness) (+ (* height 1.45) plate-thickness) wall-thickness)
       (rotate (/ Math/PI 2) [-1 0 0])
       (translate [0 (- (/ wall-thickness 2)) 0])))

(def finger-cutter-u10
  (->> (cube width height (* wall-thickness 2))
       (rotate (/ Math/PI 2) [-1 0 0])
       (translate [0 (- 0 wall-thickness plate-thickness) 0])))

(def finger-cutter-u15
  (->> (cube width (* height 1.45) wall-thickness)
       (rotate (/ Math/PI 2) [-1 0 0])
       (translate [0 (- 0 (/ wall-thickness 2) plate-thickness) 0])))

(def switch-support
  (->> (cylinder 0.5 15)
       (rotate (/ Math/PI 2) [0 1 0])))

(defn place-u10-block
  [row column block]
  (let [a (+ offset-u10 (* column step-u10))
        b (+ offset-u10 (* row step-u10))
        x (* (Math/sin a) radius)
        y (* (Math/cos a) radius)]
    (->> block
         (rotate a [0 0 -1])
         (translate [x y 0])
         (rotate (/ Math/PI 2) [0 1 0])
         (rotate b [0 0 1]))))

(defn place-u15-block
  [row column block]
  (let [a (+ offset-u10 (* column step-u10))
        b (+ offset-u15 (* row step-u15))
        x (* (Math/sin a) radius)
        y (* (Math/cos a) radius)]
    (->> block
         (rotate a [0 0 -1])
         (translate [x y 0])
         (rotate (/ Math/PI 2) [0 1 0])
         (rotate b [0 0 1]))))

(def mounting-post
  (->> (cylinder mounting-post-radius mounting-post-height)
       (with-fn 20)
       (translate [0 0 (- plate-thickness)])
       (rotate (/ Math/PI 2) [1 0 0])))

(def screw-cutter
  (->> (cylinder screw-radius mounting-post-height)
       (with-fn 20)
       (translate [0 0 (- (- wall-thickness plate-thickness))])
       (rotate (/ Math/PI 2) [1 0 0])))

(defn place-all-finger-blocks
  [block]
  (let [half-rows (/ finger-rows 2)
        half-columns (/ finger-columns 2)]
    (for [row (range (- half-rows) half-rows)
          column (range (- half-columns) half-columns)]
      (place-u10-block row column block))))


(defn place-all-thumb-blocks
  [upper-block lower-block]
  (let [half-upper-rows (/ thumb-upper-rows 2)
        half-lower-rows (/ thumb-lower-rows 2)]
    (union
      (for [row (range (- half-upper-rows) half-upper-rows) ]
        (place-u10-block row -0.5 upper-block))
      (for [row (range (- half-lower-rows) half-lower-rows)]
        (place-u15-block row 0.5 lower-block)))))

(defn place-all-posts
  [block]
  (union
    (place-u10-block -0.5 0 block)
    (place-u10-block -0.5 -1 block)
    (place-u10-block -1.5 -2 block)
    (place-u10-block -1.5 1 block)
    (place-u10-block 0.5 1 block)
    (place-u10-block 0.5 -2 block)))

(def fingers
  (->> 
  (difference
    (union (place-all-finger-blocks wall-plate-u10)
           )
    (place-all-finger-blocks finger-cutter-u10)
    (place-all-finger-blocks switch-cutter)
    )
  )
  )

(def thumbs
    (union
      (place-all-thumb-blocks wall-plate-u10 wall-plate-u15)))


(def shell-block
  (->> (cube width (+ height plate-thickness) shell-thickness)
       (rotate (/ Math/PI 2) [1 0 0])
       (translate [0 (+ (/ shell-thickness 2) thumb-y) 0])
       ))

(defn place-row-10u-blocks
  [block row column]
  (let [half-columns (/ finger-columns 2)]
    (for [row (range (- half-columns) half-columns)]
    (place-u10-block row -0.5 block))))

(def shell-end
(->> (difference (cylinder wall-thickness 104)
                            (->> (cube 100 100 110)
                                 (translate [-50 0 0])))
                (scale [1 3 1])
                (rotate (/ Math/PI 2) [0 1 0])
                (translate [0 0 (- (/ width 2))])
                (place-u10-block 1 -0.5))
  )

(defn place-reverse-block
  [block]
  (->> block
         (translate [0 (- radius) 0])
         (rotate Math/PI [0 0 1])
         (rotate Math/PI [0 1 0])
         (translate [0 (+ shell-gap) 0])
         (place-u10-block 1 -0.5))
  )


(def complete
  (union
    fingers 
    shell-end
    (place-reverse-block thumbs)
    ))

(defn render-part!
  [[filename part]]
  (spit (str "out/" filename ".scad")
        (write-scad part)))

(defn render!
  []
  (dorun 
    (map render-part!
         {"switch-cutter" switch-cutter
          "fingers" fingers
          "thumbs" thumbs
          "wall-plate-u10" wall-plate-u10
          "wall-plate-u15" wall-plate-u15
          "mounting-post" mounting-post
          "complete" complete
          })))

(render!)
