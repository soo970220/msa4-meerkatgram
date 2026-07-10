package com.msa4meerkatgram.global.responses;


import com.msa4meerkatgram.global.responses.constant.CustomResponseCode;

public record GlobalRes<T> (
    String code // 응답코드
    ,String message // 응답메세지
    ,T data  // 반환할 데이터 없을시
){  // 정상 응답을 생성하는 메서드
    // 전달받은 응답 코드와 데이터를 이용해 GlobalRes 객체를 만든다.
    public static<T> GlobalRes<T> from(CustomResponseCode customResponseCode, T data){
        return new GlobalRes<T>(customResponseCode.getCode(), customResponseCode.name(),data);
    }
    // 데이터 없이 성공만 반환하는 메서드
    // 주로 삭제(Delete)와 같이 반환 데이터가 필요 없는 경우 사용한다.
    public static GlobalRes<Void> from(CustomResponseCode customResponseCode){
        return new GlobalRes<Void>(customResponseCode.getCode(), customResponseCode.name(),null);
    }
    // 성공 응답을 간편하게 생성하는 메서드(SUCCESS를 직접 적지 않아도 되도록 만든 편의 메서드)
    // SUCCESS 코드와 함께 데이터를 반환한다.
     public static<T> GlobalRes<T>success(T data){
        return GlobalRes.<T>from(CustomResponseCode.SUCCESS,data);
    }
    // 데이터 없이 성공만 반환하는 메서드
    // 주로 삭제(Delete)와 같이 반환 데이터가 필요 없는 경우 사용한다.
    public static GlobalRes<Void> success(){
        return GlobalRes.<Void>from(CustomResponseCode.SUCCESS);

    }

}
