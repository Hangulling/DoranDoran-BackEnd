export interface User {
  email: string
  password: string
  name: string
  firstName: string
  lastName: string
  preferences: string
  createdAt: Date
  updatedAt: Date
}

// eslint-disable-next-line prefer-const
export let userProfile = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  email: 'test@example.com',
  name: 'Eun Lee',
  firstName: 'Eun',
  lastName: 'Lee',
  status: 'ACTIVE',
  role: 'ROLE_USER',
  createdAt: new Date('2025-09-01T09:00:00'),
  updatedAt: new Date('2025-09-20T14:30:00'),
}
