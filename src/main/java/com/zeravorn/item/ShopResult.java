package com.zeravorn.item;
public record ShopResult(boolean success,String errorCode,int gold){ public static ShopResult reject(String code,int gold){return new ShopResult(false,code,gold);} }
