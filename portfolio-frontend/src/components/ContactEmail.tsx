import { useState } from 'react'
import { profile } from '../data/profile'

export function ContactEmail() {
  const [message, setMessage] = useState('')
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(profile.email)
      setMessage('복사했습니다.')
    } catch {
      setMessage('복사하지 못했습니다. 이메일을 직접 선택해 주세요.')
    }
  }
  return <div className="contact-email"><a href={`mailto:${profile.email}`}>{profile.email}</a><button type="button" onClick={() => void copy()} aria-label="이메일 주소 복사">복사</button><span role="status">{message}</span></div>
}
