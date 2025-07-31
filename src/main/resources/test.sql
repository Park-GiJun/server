select *
From concerts concert
         left join concert_date c_date on concert.concert_id = c_date.concert_id
         left join concert_seat seat on c_date.concert_date_id = seat.concert_date_id
         left join concert_seat_grade grade on grade.concert_id = concert.concert_id
         left join reservation rv on rv.concert_date_id = c_date.concert_date_id
         left join users us on rv.user_id = us.user_id
         left join point_history ph on us.user_id = ph.user_id
         left join payments py on us.user_id = py.user_id and py.reservation_id = rv.reservation_id;