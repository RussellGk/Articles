package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(subStr: String, ignoreCase: Boolean = true): List<Int>{
    val result = mutableListOf<Int>()
    if (subStr.isBlank()) return result

    var indx = 0
    while (indx != -1){
        indx = this?.indexOf(subStr, indx, ignoreCase)!!
        if (indx != -1) result.add(indx++)
    }
    return result
}