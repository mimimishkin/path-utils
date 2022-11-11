# Path utils
Simple utils for svg paths and some more

## Samples

#### Svg paths

    val path1 = mutablePath().moveTo(9, 0).quadTo(12, 14, 20, 22).done()
    val path2 = path("M 9 0 Q 12 14 20 22")
    println(path1.toSvg() == path2.toSvg())

#### Operations with path

    val path = path("M18 32q-4-24 14-16L55 31q-2 13-16 12T30 54c6 8-8 15-16 6S-1 40 10 41a1 1 0 0012-6Z")
    
    // relative path
    val relative = path.toRelative() 
    
    // absolute path
    val absolute = path.toAbsolute() 
    
    // path with only "M", "L", "Q", "C", "Z"
    val simplified = path.simplify() 
    
    // 1. path with merged curves and lines that were split (it also remove redustant commands like "l 0 0")
    // 2. lines will be converted to vertical or horizontal if possible 
    // 3. curves will be converted to smooth ones if possible
    // 4. commands will be converted to relative if it's more shortly
    val minified = path.minify() 
    
    // reverse path direction
    val reversed = path.reversePath() 
    
#### Some math for bezier curve

* Intersections with another bezier or line
* Length finding
* Arc length parametrization
* Bounds finding
* Projection finding
* Curvature finding
    
#### Standard elements
    
    val circle = circle(cx = 30, cy = 30, r = 100)
    val rect = rect(x = 0, y = 0, width = 200, heigth = 100)
    val roundedRect = rect(x = 0, y = 0, width = 200, heigth = 100, rx = 20, ry = 20)
    
#### Transformations
    
    val path = ...
    val scaled = path.scale(sx = 2.0, s.y = 90.0)
    val translated = path.translate(tx = 10.0, ty = 20.0)
    val transformed = path.transformWith(Transfrorms.rotateX(PI / 4))
    
#### Additional elements
    
    val star = star(...
    val ring = ring(...
    val quadRing = quadRing(...
    val polygon = polygon(... // regular polygon
    val spiral = spiral(...
    val curve = curve(... // to make new elements from math functions
    
#### Java compatibility
    
    // Java Shape to Path and Path tp Java Shape
    val shape = path.asShape()
    val path = shape.toPath() // or pathIterator.toPath()

    // Shortcuts for Java Area Operations (add, subtract, intersect, exclusiveOr, constructor for evenOdd/nonZero shapes)
    val circle = circle(10, 10, 5)
    val star = star(10, 10, 3)
    val icon = circle difference star
    val path1 = path.nonZero()
    
    // Shortcut for Java Stroke
    val outline = path.outline(stroke)
